package FrancescoAlves.capstone.services;

import FrancescoAlves.capstone.payloads.*;
import FrancescoAlves.capstone.entities.Ingredient;
import FrancescoAlves.capstone.entities.Recipe;
import FrancescoAlves.capstone.entities.RecipeIngredient;
import FrancescoAlves.capstone.entities.IngredientSubstitutions;
import FrancescoAlves.capstone.repositories.IngredientSubstitutionRepository;
import FrancescoAlves.capstone.repositories.RecipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Cuore dello svuotafrigo. Non e' una banale "WHERE ingredient IN (...)", gira in 5 fasi:
 * 1. normalizzo l'input (stringhe libere -> Ingredient via name/slug/alias)
 * 2. aggiungo i pantry default (sale, olio, pepe dati per scontato)
 * 3. pre-filtro SQL: solo ricette con almeno 1 essenziale in dispensa
 * 4. scoring per ricetta: peso per essenziali, sostituti con score parziale
 * 5. filtro per soglia + ordinamento
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PantryMatcherService {

    private final IngredientService ingredientService;
    private final RecipeRepository recipeRepository;
    private final IngredientSubstitutionRepository substitutionRepository;

    // readOnly tiene aperta la sessione per tutto lo scoring, cosi' i lazy load
    // (ingredienti, regione) funzionano senza open-in-view e senza N+1
    // (vedi le JOIN FETCH in findCandidatesForPantry)
    @Transactional(readOnly = true)
    public PantryMatchResponse match(PantryMatchRequest req) {

        // FASE 1 - normalizzazione input
        // per ogni stringa cerco l'Ingredient su name/slug/alias; quel che non risolvo
        // lo tengo da parte per la UX ("non conosco X")
        Set<Ingredient> userPantry = new HashSet<>();
        List<String> unrecognized = new ArrayList<>();

        for (String raw : req.getIngredients()) {
            ingredientService.findByNameOrAlias(raw)
                    .ifPresentOrElse(userPantry::add, () -> unrecognized.add(raw));
        }
        log.debug("Normalizzazione: {} input, {} risolti, {} sconosciuti",
                req.getIngredients().size(), userPantry.size(), unrecognized.size());

        // FASE 2 - pantry default
        // aggiungo sale, olio, pepe, acqua, cosi' una ricetta non viene scartata
        // perche' "manca il sale"
        List<Ingredient> defaults = ingredientService.findPantryDefaults();
        userPantry.addAll(defaults);
        log.debug("Dopo defaults: dispensa di {} ingredienti", userPantry.size());

        Set<Long> pantryIds = userPantry.stream()
                .map(Ingredient::getId)
                .collect(Collectors.toSet());

        // ID dei soli default: servono per NON suggerire ricette coperte solo da questi.
        // L'utente deve aver messo almeno un ingrediente "vero" che matcha.
        Set<Long> defaultIds = defaults.stream()
                .map(Ingredient::getId)
                .collect(Collectors.toSet());

        // dispensa vuota: l'utente ha scritto solo cose sconosciute
        if (pantryIds.isEmpty()) {
            return PantryMatchResponse.builder()
                    .matches(List.of())
                    .unrecognizedInputs(unrecognized)
                    .totalCandidatesEvaluated(0)
                    .build();
        }

        // FASE 3 - pre-filter SQL
        // solo le ricette con almeno un essenziale in dispensa, senno' dovrei scansionare
        // tutto il DB in Java. Se non vuole sostituzioni passo minScore=2.0 (irraggiungibile)
        // cosi' il pre-filtro non allarga ai match coperti solo da sostituti.
        boolean allowSubs = Boolean.TRUE.equals(req.getAllowSubstitutions());
        double minSubScore = req.getMinSubstitutionScore() != null ? req.getMinSubstitutionScore() : 0.7;
        List<Recipe> candidates = recipeRepository.findCandidatesForPantry(
                pantryIds, allowSubs ? minSubScore : 2.0);
        log.debug("Pre-filter: {} ricette candidate", candidates.size());

        // FASE 4 - scoring per ricetta
        List<RecipeMatchResult> results = new ArrayList<>();

        for (Recipe recipe : candidates) {
            RecipeMatchResult result = scoreRecipe(recipe, userPantry, pantryIds, defaultIds, req);
            if (result != null) {
                results.add(result);
            }
        }

        // FASE 5 - filtro per soglia + ordinamento
        List<RecipeMatchResult> finalMatches = results.stream()
                .filter(r -> r.getMatchPercent() >= req.getMinMatchPercent())
                // matchPercent DESC, poi meno mancanti (meno spesa = meglio)
                .sorted(Comparator
                        .comparingDouble(RecipeMatchResult::getMatchPercent).reversed()
                        .thenComparingInt(r -> r.getMissingIngredients().size()))
                .limit(req.getLimit())
                .toList();

        return PantryMatchResponse.builder()
                .matches(finalMatches)
                .unrecognizedInputs(unrecognized)
                .totalCandidatesEvaluated(candidates.size())
                .build();
    }

    // per ogni essenziale della ricetta: in dispensa -> coperto 1.0; sostituto in
    // dispensa -> coperto allo score (es. 0.85); altrimenti mancante.
    // matchPercent = somma copertura / numero essenziali * 100.
    // Es: 5 essenziali, 3 in dispensa + 1 via sostituto a 0.8 -> 3.8/5*100 = 76%
    private RecipeMatchResult scoreRecipe(Recipe recipe,
            Set<Ingredient> userPantry,
            Set<Long> pantryIds,
            Set<Long> defaultIds,
            PantryMatchRequest req) {

        // solo essenziali: i decorativi non contano (non penalizzo per "manca il prezzemolo")
        List<RecipeIngredient> essentials = recipe.getRecipeIngredients().stream()
                .filter(RecipeIngredient::isEssential)
                .toList();

        if (essentials.isEmpty()) {
            return null; // ricetta senza essenziali, skip
        }

        double coveredScore = 0.0;
        List<String> coveredNames = new ArrayList<>();
        List<String> missingNames = new ArrayList<>();
        List<SubstitutionInfo> substitutionsUsed = new ArrayList<>();
        // almeno un essenziale coperto da un ingrediente "vero" (non un default),
        // senno' non suggerisco la ricetta
        boolean hasRealMatch = false;

        for (RecipeIngredient ri : essentials) {
            Ingredient required = ri.getIngredient();

            // caso A: richiesto esattamente in dispensa
            if (pantryIds.contains(required.getId())) {
                coveredScore += 1.0;
                coveredNames.add(required.getName());
                if (!defaultIds.contains(required.getId()))
                    hasRealMatch = true;
                continue;
            }

            // caso B: non c'e', ma forse ho un sostituto valido
            if (Boolean.TRUE.equals(req.getAllowSubstitutions())) {
                Optional<SubMatch> bestSub = findBestSubstituteInPantry(
                        required.getId(), pantryIds,
                        req.getMinSubstitutionScore() != null ? req.getMinSubstitutionScore() : 0.7);
                if (bestSub.isPresent()) {
                    SubMatch sub = bestSub.get();
                    coveredScore += sub.score();
                    coveredNames.add(required.getName() + " (via " + sub.substitute().getName() + ")");
                    substitutionsUsed.add(SubstitutionInfo.builder()
                            .requiredIngredient(required.getName())
                            .substituteUsed(sub.substitute().getName())
                            .compatibilityScore(sub.score())
                            .note(sub.note())
                            .build());
                    if (!defaultIds.contains(sub.substitute().getId()))
                        hasRealMatch = true;
                    continue;
                }
            }

            // caso C: mancante
            missingNames.add(required.getName());
        }

        // coperta solo dai default (sale/olio/acqua): non la suggerisco
        if (!hasRealMatch) {
            return null;
        }

        double matchPercent = (coveredScore / essentials.size()) * 100.0;

        return RecipeMatchResult.builder()
                .recipe(toSummary(recipe))
                .matchPercent(Math.round(matchPercent * 100.0) / 100.0)
                .coveredIngredients(coveredNames)
                .missingIngredients(missingNames)
                .substitutions(substitutionsUsed)
                .build();
    }

    // sostituto con score migliore che sia in dispensa.
    // findValidSubstitutes torna i sostituti gia' ordinati per score DESC e con
    // score >= minScore; prendo il primo che e' anche in dispensa.
    private Optional<SubMatch> findBestSubstituteInPantry(Long requiredIngredientId,
            Set<Long> pantryIds,
            double minScore) {
        // bidirezionale: il richiesto puo' stare su uno dei due lati della regola,
        // il sostituto da cercare e' sempre "l'altro lato"
        List<IngredientSubstitutions> candidates = substitutionRepository
                .findValidSubstitutes(requiredIngredientId, minScore);
        for (IngredientSubstitutions s : candidates) {
            Ingredient other = s.getIngredient().getId().equals(requiredIngredientId)
                    ? s.getSubstitutions()
                    : s.getIngredient();
            if (pantryIds.contains(other.getId())) {
                return Optional.of(new SubMatch(other, s.getCompatibilityScore(), s.getNote()));
            }
        }
        return Optional.empty();
    }

    // sostituto risolto: l'ingrediente "altro lato" della regola + score + nota
    private record SubMatch(Ingredient substitute, double score, String note) {
    }

    private RecipeSummaryDto toSummary(Recipe r) {
        return RecipeSummaryDto.builder()
                .id(r.getId())
                .title(r.getTitle())
                .description(r.getDescription())
                .difficulty(r.getDifficulty())
                .prepTimeMinutes(r.getPrepTimeMinutes())
                .cookTimeMinutes(r.getCookTimeMinutes())
                .servings(r.getServings())
                .regionName(r.getRegion() != null ? r.getRegion().getName() : null)
                .imageUrl(r.getImageUrl())
                .type(r.getRecipeType())
                .averageRating(r.getAverageRating())
                .reviewCount(r.getReviewCount())
                .build();
    }
}

package FrancescoAlves.capstone.services;

import FrancescoAlves.capstone.payloads.AliasDto;
import FrancescoAlves.capstone.payloads.CreateIngredientAliasRequest;
import FrancescoAlves.capstone.payloads.CreateIngredientRequest;
import FrancescoAlves.capstone.payloads.CreateIngredientSubstitutionRequest;
import FrancescoAlves.capstone.payloads.SubstitutionDto;
import FrancescoAlves.capstone.entities.Ingredient;
import FrancescoAlves.capstone.entities.IngredientAlias;
import FrancescoAlves.capstone.entities.IngredientSubstitutions;
import FrancescoAlves.capstone.exceptions.NotFoundException;
import FrancescoAlves.capstone.exceptions.ValidationException;
import FrancescoAlves.capstone.repositories.IngredientAliasRepository;
import FrancescoAlves.capstone.repositories.IngredientRepository;
import FrancescoAlves.capstone.repositories.IngredientSubstitutionRepository;
import FrancescoAlves.capstone.repositories.RecipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final IngredientAliasRepository aliasRepository;
    private final IngredientSubstitutionRepository substitutionRepository;
    private final RecipeRepository recipeRepository;

    public List<Ingredient> findAll() {
        return ingredientRepository.findAll();
    }

    public Ingredient findById(Long id) {
        return ingredientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ingredient", id));
    }

    // lookup per il pantry matcher: cerca per nome, slug o alias.
    // Optional perche' l'utente puo' scrivere qualunque cosa
    public Optional<Ingredient> findByNameOrAlias(String term) {
        return ingredientRepository.findByNameOrAlias(term.trim());
    }

    public List<Ingredient> findPantryDefaults() {
        return ingredientRepository.findByIsPantryDefaultTrue();
    }

    public Ingredient create(CreateIngredientRequest req) {
        String name = capitalize(req.getName().trim());
        // slug univoco: se l'admin non lo passa lo calcolo dal name
        String slug = (req.getSlug() != null && !req.getSlug().isBlank())
                ? req.getSlug().trim().toLowerCase()
                : slugify(name);

        // anche il name e' UNIQUE: controllo qui per dare un 400 chiaro invece di un 500
        if (ingredientRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new ValidationException("Esiste gia' un ingrediente con nome '" + name + "'");
        }
        if (ingredientRepository.findBySlug(slug).isPresent()) {
            throw new ValidationException("Esiste gia' un ingrediente con slug " + slug);
        }
        Ingredient ing = Ingredient.builder()
                .name(name)
                .slug(slug)
                .category(req.getCategory())
                .isPantryDefault(Boolean.TRUE.equals(req.getIsPantryDefault()))
                .build();
        ing = ingredientRepository.save(ing);
        log.info("Ingredient creato: {} ({})", ing.getName(), ing.getSlug());

        // alias opzionali passati in creazione: salto vuoti, duplicati e collisioni
        // con alias esistenti senza far fallire la creazione
        if (req.getAliases() != null) {
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (String raw : req.getAliases()) {
                if (raw == null || raw.isBlank())
                    continue;
                String aliasText = raw.trim().toLowerCase();
                if (!seen.add(aliasText))
                    continue;
                if (ingredientRepository.findByNameOrAlias(aliasText).isPresent())
                    continue;
                aliasRepository.save(IngredientAlias.builder().ingredient(ing).alias(aliasText).build());
            }
        }
        return ing;
    }

    public Ingredient update(Long id, CreateIngredientRequest req) {
        Ingredient ing = findById(id);
        String newName = capitalize(req.getName().trim());
        String newSlug = (req.getSlug() != null && !req.getSlug().isBlank())
                ? req.getSlug().trim().toLowerCase()
                : slugify(newName);

        // se cambia il name verifico che non sia di un altro ingrediente
        if (!ing.getName().equalsIgnoreCase(newName)) {
            ingredientRepository.findByNameIgnoreCase(newName).ifPresent(other -> {
                throw new ValidationException("Nome gia' in uso da un altro ingrediente");
            });
        }
        // stessa cosa per lo slug
        if (!ing.getSlug().equals(newSlug)
                && ingredientRepository.findBySlug(newSlug).isPresent()) {
            throw new ValidationException("Slug gia' in uso da un altro ingrediente");
        }
        ing.setName(newName);
        ing.setSlug(newSlug);
        ing.setCategory(req.getCategory());
        ing.setPantryDefault(Boolean.TRUE.equals(req.getIsPantryDefault()));
        Ingredient updated = ingredientRepository.save(ing);
        log.info("Ingredient {} aggiornato", id);
        return updated;
    }

    public void delete(Long id) {
        Ingredient ing = findById(id);
        // non cancellabile se usato in una ricetta, altrimenti recipe_ingredients orfani con FK violation
        if (recipeRepository.isIngredientUsedInAnyRecipe(id)) {
            throw new ValidationException(
                    "Impossibile cancellare ingrediente: e' usato in una o piu' ricette");
        }
        ingredientRepository.delete(ing);
        log.info("Ingredient {} cancellato", id);
    }

    public IngredientAlias createAlias(Long ingredientId, CreateIngredientAliasRequest req) {
        Ingredient ing = findById(ingredientId);
        String aliasText = req.getAlias().trim().toLowerCase();

        // alias UNIQUE globalmente: lo stesso testo non puo' puntare a due ingredienti
        if (ingredientRepository.findByNameOrAlias(aliasText).isPresent()) {
            throw new ValidationException("Alias '" + aliasText + "' gia' presente nel sistema");
        }
        IngredientAlias alias = IngredientAlias.builder()
                .ingredient(ing).alias(aliasText)
                .build();
        alias = aliasRepository.save(alias);
        log.info("Alias '{}' aggiunto a ingredient {}", aliasText, ing.getName());
        return alias;
    }

    public void deleteAlias(Long aliasId) {
        IngredientAlias alias = aliasRepository.findById(aliasId)
                .orElseThrow(() -> new NotFoundException("IngredientAlias", aliasId));
        aliasRepository.delete(alias);
        log.info("Alias {} cancellato", aliasId);
    }

    public IngredientSubstitutions createSubstitution(CreateIngredientSubstitutionRequest req) {
        if (req.getIngredientId().equals(req.getSubstituteId())) {
            throw new ValidationException("Un ingrediente non puo' sostituire se stesso");
        }
        // regola bidirezionale: se esiste gia' tra i due (in qualunque verso) e' un doppione
        if (substitutionRepository.existsBetween(req.getIngredientId(), req.getSubstituteId())) {
            throw new ValidationException("Esiste gia' una regola di sostituzione tra questi due ingredienti");
        }
        Ingredient ing = findById(req.getIngredientId());
        Ingredient sub = findById(req.getSubstituteId());

        IngredientSubstitutions s = IngredientSubstitutions.builder()
                .ingredient(ing).substitutions(sub)
                .compatibilityScore(req.getCompatibilityScore())
                .note(req.getNote())
                .build();
        s = substitutionRepository.save(s);
        log.info("Substitution creata: {} -> {} (score {})",
                ing.getName(), sub.getName(), req.getCompatibilityScore());
        return s;
    }

    public void deleteSubstitution(Long id) {
        IngredientSubstitutions s = substitutionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("IngredientSubstitution", id));
        substitutionRepository.delete(s);
        log.info("Substitution {} cancellata", id);
    }

    public List<AliasDto> getAliases(Long ingredientId) {
        findById(ingredientId); // 404 se l'ingrediente non esiste
        return aliasRepository.findByIngredientId(ingredientId).stream()
                .map(a -> new AliasDto(a.getId(), a.getAlias()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubstitutionDto> getAllSubstitutions() {
        return substitutionRepository.findAll().stream()
                .map(s -> new SubstitutionDto(
                        s.getId(),
                        s.getIngredient().getId(), s.getIngredient().getName(),
                        s.getSubstitutions().getId(), s.getSubstitutions().getName(),
                        s.getCompatibilityScore(), s.getNote()))
                .toList();
    }

    // prima lettera maiuscola, resto invariato ("mais" -> "Mais")
    private static String capitalize(String s) {
        if (s == null || s.isEmpty())
            return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private String slugify(String name) {
        return name.trim().toLowerCase()
                .replaceAll("[\\s+]", "-")
                .replaceAll("[^a-z0-9\\-]", "");
    }
}

package FrancescoAlves.capstone.services;

import FrancescoAlves.capstone.payloads.*;
import FrancescoAlves.capstone.entities.*;
import FrancescoAlves.capstone.enums.RecipeType;
import FrancescoAlves.capstone.exceptions.NotFoundException;
import FrancescoAlves.capstone.exceptions.ValidationException;
import FrancescoAlves.capstone.repositories.IngredientRepository;
import FrancescoAlves.capstone.repositories.RecipeRepository;
import FrancescoAlves.capstone.repositories.RegionRepository;
import FrancescoAlves.capstone.repositories.ReviewRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final RegionRepository regionRepository;
    private final IngredientRepository ingredientRepository;
    private final ReviewRepository reviewRepository;

    public Page<RecipeSummaryDto> findAll(Pageable pageable) {
        return recipeRepository.findAll(pageable).map(this::toSummary);
    }

    public Page<RecipeSummaryDto> findByRegion(Long regionId, Pageable pageable) {
        return recipeRepository.findByRegionId(regionId, pageable).map(this::toSummary);
    }

    public RecipeDetailDto findById(UUID id) {
        return toDetail(getEntityById(id));
    }

    public Recipe getEntityById(UUID id) {
        return recipeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Recipe", id));
    }

    public Page<RecipeSummaryDto> findByIngredientId(Long ingredientId, Pageable pageable) {
        return recipeRepository.findByIngredientId(ingredientId, pageable).map(this::toSummary);
    }

    // ricerca per nome/slug/alias, es. "pomodorini" -> ricette con "Pomodorini ciliegino"
    public Page<RecipeSummaryDto> findByIngredientTerm(String term, Pageable pageable) {
        if (term == null || term.isBlank()) {
            throw new ValidationException("Termine di ricerca obbligatorio");
        }
        return recipeRepository.findByIngredientNameOrAlias(term.trim(), pageable).map(this::toSummary);
    }

    // transazionale: se qualcosa fallisce a meta', rollback completo
    @Transactional
    public RecipeDetailDto create(CreateRecipeRequest req) {
        Region region = null;
        if (req.getRegionId() != null) {
            region = regionRepository.findById(req.getRegionId())
                    .orElseThrow(() -> new NotFoundException("Region", req.getRegionId()));
        }

        // serve almeno un essenziale, altrimenti lo svuotafrigo non potrebbe mai matcharla
        boolean hasEssential = req.getIngredients().stream()
                .anyMatch(i -> Boolean.TRUE.equals(i.getIsEssential()));
        if (!hasEssential) {
            throw new ValidationException("La ricetta deve avere almeno un ingrediente essenziale");
        }

        RecipeType type = req.getType() != null ? req.getType() : RecipeType.TRADITIONAL;
        Recipe recipe = Recipe.builder()
                .title(capitalize(req.getTitle()))
                .description(req.getDescription())
                .difficulty(req.getDifficulty())
                .prepTimeMinutes(req.getPrepTimeMinutes())
                .cookTimeMinutes(req.getCookTimeMinutes())
                .servings(req.getServings())
                .imageUrl(req.getImageUrl())
                .region(region)
                .recipeType(type)
                .build();

        // verifico l'esistenza di ogni Ingredient
        for (CreateRecipeIngredientRequest ri : req.getIngredients()) {
            Ingredient ingredient = ingredientRepository.findById(ri.getIngredientId())
                    .orElseThrow(() -> new NotFoundException("Ingredient", ri.getIngredientId()));
            RecipeIngredient line = RecipeIngredient.builder()
                    .recipe(recipe)
                    .ingredient(ingredient)
                    .quantity(ri.getQuantity())
                    .unit(ri.getUnit())
                    .isEssential(Boolean.TRUE.equals(ri.getIsEssential()))
                    .notes(ri.getNote())
                    .build();
            recipe.getRecipeIngredients().add(line);
        }

        // step ordinati per step_order
        List<CreateRecipeStepRequest> sortedSteps = req.getSteps().stream()
                .sorted(Comparator.comparing(CreateRecipeStepRequest::getStepOrder))
                .toList();
        // niente step_order duplicati
        long distinctOrders = sortedSteps.stream().map(CreateRecipeStepRequest::getStepOrder).distinct().count();
        if (distinctOrders != sortedSteps.size()) {
            throw new ValidationException("Step con step_order duplicato");
        }
        for (CreateRecipeStepRequest s : sortedSteps) {
            RecipeStep step = RecipeStep.builder()
                    .recipe(recipe)
                    .stepOrder(s.getStepOrder())
                    .description(s.getDescription())
                    .build();
            recipe.getRecipeSteps().add(step);
        }

        // cascade ALL propaga su recipe_ingredients e recipe_steps
        Recipe saved = recipeRepository.save(recipe);
        log.info("Recipe creata: {} ({})", saved.getTitle(), saved.getId());
        return toDetail(saved);
    }

    // full replace: ingredienti e step vengono rimossi e ricreati da capo
    // (il client manda l'intera struttura aggiornata). Le review restano, sono indipendenti.
    @Transactional
    public RecipeDetailDto update(UUID id, CreateRecipeRequest req) {
        Recipe recipe = getEntityById(id);

        if (req.getRegionId() != null) {
            Region region = regionRepository.findById(req.getRegionId())
                    .orElseThrow(() -> new NotFoundException("Region", req.getRegionId()));
            recipe.setRegion(region);
        } else {
            recipe.setRegion(null);
        }

        recipe.setTitle(capitalize(req.getTitle()));
        recipe.setDescription(req.getDescription());
        recipe.setDifficulty(req.getDifficulty());
        recipe.setPrepTimeMinutes(req.getPrepTimeMinutes());
        recipe.setCookTimeMinutes(req.getCookTimeMinutes());
        recipe.setServings(req.getServings());
        recipe.setImageUrl(req.getImageUrl());
        if (req.getType() != null) {
            recipe.setRecipeType(req.getType());
        }

        // validazioni prima di toccare le collezioni (fail-fast)
        boolean hasEssential = req.getIngredients().stream()
                .anyMatch(i -> Boolean.TRUE.equals(i.getIsEssential()));
        if (!hasEssential) {
            throw new ValidationException("La ricetta deve avere almeno un ingrediente essenziale");
        }
        List<CreateRecipeStepRequest> sortedSteps = req.getSteps().stream()
                .sorted(Comparator.comparing(CreateRecipeStepRequest::getStepOrder))
                .toList();
        long distinctOrders = sortedSteps.stream().map(CreateRecipeStepRequest::getStepOrder).distinct().count();
        if (distinctOrders != sortedSteps.size()) {
            throw new ValidationException("Step con step_order duplicato");
        }

        // svuoto e flusho PRIMA di reinserire: senno' Hibernate fa le INSERT prima delle
        // DELETE orfane nello stesso flush e le righe nuove collidono con le vecchie sugli
        // unique (recipe_id+ingredient_id, recipe_id+step_order) -> violazione vincolo -> 500
        recipe.getRecipeIngredients().clear();
        recipe.getRecipeSteps().clear();
        recipeRepository.flush();

        for (CreateRecipeIngredientRequest ri : req.getIngredients()) {
            Ingredient ingredient = ingredientRepository.findById(ri.getIngredientId())
                    .orElseThrow(() -> new NotFoundException("Ingredient", ri.getIngredientId()));
            recipe.getRecipeIngredients().add(RecipeIngredient.builder()
                    .recipe(recipe)
                    .ingredient(ingredient)
                    .quantity(ri.getQuantity())
                    .unit(ri.getUnit())
                    .isEssential(Boolean.TRUE.equals(ri.getIsEssential()))
                    .notes(ri.getNote())
                    .build());
        }
        for (CreateRecipeStepRequest s : sortedSteps) {
            recipe.getRecipeSteps().add(RecipeStep.builder()
                    .recipe(recipe)
                    .stepOrder(s.getStepOrder())
                    .description(s.getDescription())
                    .build());
        }

        Recipe updated = recipeRepository.save(recipe);
        log.info("Recipe {} aggiornata", id);
        return toDetail(updated);
    }

    @Transactional
    public void delete(UUID id) {
        Recipe recipe = getEntityById(id);
        // prima le review: senza ON DELETE CASCADE resterebbero orfane. Niente @OneToMany
        // cascade su Recipe per non caricare N review ogni volta, quindi delete esplicita.
        long reviewCount = reviewRepository.countByRecipeId(id);
        if (reviewCount > 0) {
            reviewRepository.deleteByRecipeId(id);
            log.info("Cancellate {} review della ricetta {}", reviewCount, id);
        }
        // cascade ALL elimina anche recipe_ingredients e recipe_steps
        recipeRepository.delete(recipe);
        log.info("Recipe {} cancellata", id);
    }

    // prima lettera maiuscola, resto invariato ("carbonara" -> "Carbonara")
    private static String capitalize(String s) {
        if (s == null || s.isBlank())
            return s;
        String t = s.trim();
        return t.substring(0, 1).toUpperCase() + t.substring(1);
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

    private RecipeDetailDto toDetail(Recipe r) {
        List<RecipeIngredientDto> ingredients = r.getRecipeIngredients().stream()
                .map(ri -> RecipeIngredientDto.builder()
                        .ingredientId(ri.getIngredient().getId())
                        .ingredientName(ri.getIngredient().getName())
                        .quantity(ri.getQuantity())
                        .unit(ri.getUnit())
                        .isEssential(ri.isEssential())
                        .note(ri.getNotes())
                        .build())
                .toList();

        List<RecipeStepDto> steps = r.getRecipeSteps().stream()
                .sorted(Comparator.comparing(RecipeStep::getStepOrder))
                .map(s -> RecipeStepDto.builder()
                        .stepOrder(s.getStepOrder())
                        .description(s.getDescription())
                        .build())
                .toList();

        return RecipeDetailDto.builder()
                .id(r.getId())
                .title(r.getTitle())
                .description(r.getDescription())
                .difficulty(r.getDifficulty())
                .prepTimeMinutes(r.getPrepTimeMinutes())
                .cookTimeMinutes(r.getCookTimeMinutes())
                .servings(r.getServings())
                .regionId(r.getRegion() != null ? r.getRegion().getId() : null)
                .regionName(r.getRegion() != null ? r.getRegion().getName() : null)
                .imageUrl(r.getImageUrl())
                .type(r.getRecipeType())
                .averageRating(r.getAverageRating())
                .reviewCount(r.getReviewCount())
                .ingredients(ingredients)
                .steps(steps)
                .build();
    }
}

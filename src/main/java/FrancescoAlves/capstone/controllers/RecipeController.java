package FrancescoAlves.capstone.controllers;

import FrancescoAlves.capstone.payloads.*;
import FrancescoAlves.capstone.services.RecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    // es. ?sort=averageRating,desc
    @GetMapping
    public Page<RecipeSummaryDto> getAll(Pageable pageable) {
        return recipeService.findAll(pageable);
    }

    @GetMapping("/by-region/{regionId}")
    public Page<RecipeSummaryDto> getByRegion(@PathVariable Long regionId, Pageable pageable) {
        return recipeService.findByRegion(regionId, pageable);
    }

    // quando hai gia' l'id dell'ingrediente (es. da pagina dettaglio)
    @GetMapping("/by-ingredient/{ingredientId}")
    public Page<RecipeSummaryDto> getByIngredient(
            @PathVariable Long ingredientId,
            Pageable pageable) {
        return recipeService.findByIngredientId(ingredientId, pageable);
    }

    // qui l'utente scrive il nome o un alias
    @GetMapping("/search-by-ingredient")
    public Page<RecipeSummaryDto> searchByIngredient(
            @RequestParam String term,
            Pageable pageable) {
        return recipeService.findByIngredientTerm(term, pageable);
    }

    @GetMapping("/{id}")
    public RecipeDetailDto getById(@PathVariable UUID id) {
        return recipeService.findById(id);
    }

    @PostMapping
    public ResponseEntity<RecipeDetailDto> create(@Valid @RequestBody CreateRecipeRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recipeService.create(req));
    }

    @PutMapping("/{id}")
    public RecipeDetailDto update(@PathVariable UUID id, @Valid @RequestBody CreateRecipeRequest req) {
        return recipeService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        recipeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
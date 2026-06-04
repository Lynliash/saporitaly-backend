package FrancescoAlves.capstone.controllers;

import FrancescoAlves.capstone.payloads.*;
import FrancescoAlves.capstone.entities.Ingredient;
import FrancescoAlves.capstone.entities.IngredientAlias;
import FrancescoAlves.capstone.entities.IngredientSubstitutions;
import FrancescoAlves.capstone.services.IngredientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// raggruppa ingredients, aliases e substitutions: niente @RequestMapping di classe
// perche' i path sono diversi
@RestController
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    @GetMapping("/ingredients")
    public List<Ingredient> getAll() {
        return ingredientService.findAll();
    }

    @GetMapping("/ingredients/{id}")
    public Ingredient getById(@PathVariable Long id) {
        return ingredientService.findById(id);
    }

    // alias di un ingrediente, serve al backoffice per listarli/cancellarli
    @GetMapping("/ingredients/{ingredientId}/aliases")
    public List<AliasDto> getAliases(@PathVariable Long ingredientId) {
        return ingredientService.getAliases(ingredientId);
    }

    @PostMapping("/ingredients")
    public ResponseEntity<Ingredient> create(@Valid @RequestBody CreateIngredientRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ingredientService.create(req));
    }

    @PutMapping("/ingredients/{id}")
    public Ingredient update(@PathVariable Long id, @Valid @RequestBody CreateIngredientRequest req) {
        return ingredientService.update(id, req);
    }

    @DeleteMapping("/ingredients/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ingredientService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/ingredients/{ingredientId}/aliases")
    public ResponseEntity<IngredientAlias> createAlias(
            @PathVariable Long ingredientId,
            @Valid @RequestBody CreateIngredientAliasRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ingredientService.createAlias(ingredientId, req));
    }

    @DeleteMapping("/aliases/{aliasId}")
    public ResponseEntity<Void> deleteAlias(@PathVariable Long aliasId) {
        ingredientService.deleteAlias(aliasId);
        return ResponseEntity.noContent().build();
    }

    // tutte le regole di sostituzione, per il backoffice
    @GetMapping("/substitutions")
    public List<SubstitutionDto> getSubstitutions() {
        return ingredientService.getAllSubstitutions();
    }

    @PostMapping("/substitutions")
    public ResponseEntity<IngredientSubstitutions> createSubstitution(
            @Valid @RequestBody CreateIngredientSubstitutionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ingredientService.createSubstitution(req));
    }

    @DeleteMapping("/substitutions/{id}")
    public ResponseEntity<Void> deleteSubstitution(@PathVariable Long id) {
        ingredientService.deleteSubstitution(id);
        return ResponseEntity.noContent().build();
    }
}

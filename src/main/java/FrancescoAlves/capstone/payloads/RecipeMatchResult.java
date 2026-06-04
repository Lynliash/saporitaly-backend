package FrancescoAlves.capstone.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

// Output: per ogni ricetta che ha matchato, il dettaglio della copertura.
@Data
@Builder
@AllArgsConstructor
public class RecipeMatchResult {
    private RecipeSummaryDto recipe;
    private Double matchPercent;              // 0-100
    private List<String> coveredIngredients;
    private List<String> missingIngredients;
    private List<SubstitutionInfo> substitutions;
}
package FrancescoAlves.capstone.payloads;


import FrancescoAlves.capstone.enums.IngredientUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class RecipeIngredientDto {
    private Long ingredientId;
    private String ingredientName;
    private Double quantity;
    private IngredientUnit unit;
    private Boolean isEssential;
    private String note;
}
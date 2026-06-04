package FrancescoAlves.capstone.payloads;


import FrancescoAlves.capstone.enums.IngredientUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateRecipeIngredientRequest {

    @NotNull
    private Long ingredientId;     // Ingredient gia' nel catalogo

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "quantita' > 0")
    private Double quantity;

    @NotNull
    private IngredientUnit unit;

    // Default true se omesso (il service forza TRUE.equals).
    private Boolean isEssential = true;

    @Size(max = 200)
    private String note;
}
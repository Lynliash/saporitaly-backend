package FrancescoAlves.capstone.payloads;


import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

// Body per POST /substitutions. Solo ADMIN.
@Data
public class CreateIngredientSubstitutionRequest {

    @NotNull
    private Long ingredientId;        // l'ingrediente richiesto

    @NotNull
    private Long substituteId;        // l'ingrediente che lo puo' sostituire

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true, message = "score minimo 0")
    @DecimalMax(value = "1.0", inclusive = true, message = "score massimo 1")
    private Double compatibilityScore;

    @Size(max = 200)
    private String note;
}

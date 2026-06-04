package FrancescoAlves.capstone.payloads;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateRecipeStepRequest {

    @NotNull
    @Min(1)
    private Integer stepOrder;

    @NotBlank
    private String description;
}

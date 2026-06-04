package FrancescoAlves.capstone.payloads;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// Body per POST /ingredients/{id}/aliases. Solo ADMIN.
@Data
public class CreateIngredientAliasRequest {

    @NotBlank(message = "alias obbligatorio")
    @Size(min = 2, max = 100)
    private String alias;
}
package FrancescoAlves.capstone.payloads;

import FrancescoAlves.capstone.enums.IngredientCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

// Body per POST/PUT su /ingredients. Solo ADMIN.
@Data
public class CreateIngredientRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    // Se omesso viene derivato dal name (slugify).
    @Size(max = 100)
    private String slug;

    @NotNull
    private IngredientCategory category;

    // Se true lo svuotafrigo lo mette in dispensa a tutti di default.
    private Boolean isPantryDefault = false;

    // Alias da creare insieme all'ingrediente.
    // Quelli vuoti o gia' presenti vengono ignorati, senza errore.
    private List<String> aliases;
}

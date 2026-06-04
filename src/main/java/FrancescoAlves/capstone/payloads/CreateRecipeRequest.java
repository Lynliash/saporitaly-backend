package FrancescoAlves.capstone.payloads;


import FrancescoAlves.capstone.enums.Difficulty;
import FrancescoAlves.capstone.enums.RecipeType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

// Body per POST/PUT su /recipes. Per ora solo ADMIN.
@Data
public class CreateRecipeRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    private String description;

    @NotNull
    private Difficulty difficulty;

    @NotNull
    @Min(0)
    private Integer prepTimeMinutes;

    @NotNull
    @Min(0)
    private Integer cookTimeMinutes;

    @NotNull
    @Min(1)
    private Integer servings;

    // Opzionale, ma per le TRADITIONAL meglio fornirla sempre.
    private Long regionId;

    // URL immagine (es. Cloudinary), opzionale.
    @Size(max = 500)
    private String imageUrl;

    // Opzionale: se omesso il service mette TRADITIONAL.
    private RecipeType type;

    @NotEmpty(message = "almeno un ingrediente")
    @Valid
    private List<CreateRecipeIngredientRequest> ingredients;

    @NotEmpty(message = "almeno uno step")
    @Valid
    private List<CreateRecipeStepRequest> steps;
}

package FrancescoAlves.capstone.payloads;

import FrancescoAlves.capstone.enums.Difficulty;
import FrancescoAlves.capstone.enums.RecipeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

// Dettaglio per la pagina singola ricetta: con ingredienti e step.
@Data
@Builder
@AllArgsConstructor
public class RecipeDetailDto {
    private UUID id;
    private String title;
    private String description;
    private Difficulty difficulty;
    private Integer prepTimeMinutes;
    private Integer cookTimeMinutes;
    private Integer servings;
    private Long regionId;
    private String regionName;
    private String imageUrl;
    private RecipeType type;
    private Double averageRating;
    private Integer reviewCount;
    private List<RecipeIngredientDto> ingredients;
    private List<RecipeStepDto> steps;
}

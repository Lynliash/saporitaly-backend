package FrancescoAlves.capstone.payloads;

import FrancescoAlves.capstone.enums.Difficulty;
import FrancescoAlves.capstone.enums.RecipeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

// Versione leggera per le liste, senza ingredienti/steps (sarebbero pesanti).
@Data
@Builder
@AllArgsConstructor
public class RecipeSummaryDto {
    private UUID id;
    private String title;
    private String description;
    private Difficulty difficulty;
    private Integer prepTimeMinutes;
    private Integer cookTimeMinutes;
    private Integer servings;
    private String regionName;
    private String imageUrl;
    private RecipeType type;
    private Double averageRating;
    private Integer reviewCount;
}

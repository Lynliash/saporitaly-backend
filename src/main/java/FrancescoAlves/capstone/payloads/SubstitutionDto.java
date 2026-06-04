package FrancescoAlves.capstone.payloads;

import lombok.AllArgsConstructor;
import lombok.Data;

// Sostituzione appiattita per l'admin (lista + delete).
@Data
@AllArgsConstructor
public class SubstitutionDto {
    private Long id;
    private Long ingredientId;
    private String ingredientName;
    private Long substituteId;
    private String substituteName;
    private double compatibilityScore;
    private String note;
}

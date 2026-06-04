package FrancescoAlves.capstone.payloads;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class SubstitutionInfo {
    private String requiredIngredient;
    private String substituteUsed;
    private Double compatibilityScore;
    private String note;
}

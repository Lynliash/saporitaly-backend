package FrancescoAlves.capstone.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class RecipeStepDto {
    private Integer stepOrder;
    private String description;
}

package FrancescoAlves.capstone.payloads;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AliasDto {
    private Long id;
    private String alias;
}

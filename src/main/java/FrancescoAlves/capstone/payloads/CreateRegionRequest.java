package FrancescoAlves.capstone.payloads;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// Body per POST/PUT su /regions. Solo ADMIN.
@Data
public class CreateRegionRequest {

    @NotBlank(message = "name obbligatorio")
    @Size(max = 50)
    private String name;

    @NotBlank(message = "code obbligatorio")
    @Size(min = 2, max = 5)
    private String code;

    @Size(max = 500)
    private String description;
}
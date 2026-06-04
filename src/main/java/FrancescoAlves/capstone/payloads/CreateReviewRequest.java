package FrancescoAlves.capstone.payloads;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateReviewRequest {

    @NotNull
    @Min(value = 1, message = "rating minimo 1")
    @Max(value = 10, message = "rating massimo 10")
    private Integer rating;

    private String comment;
}
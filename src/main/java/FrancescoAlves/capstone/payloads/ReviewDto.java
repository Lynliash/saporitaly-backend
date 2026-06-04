package FrancescoAlves.capstone.payloads;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class ReviewDto {
    private UUID id;
    private UUID recipeId;
    private UUID userId;
    private String username;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
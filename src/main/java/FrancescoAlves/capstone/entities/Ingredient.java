package FrancescoAlves.capstone.entities;

import FrancescoAlves.capstone.enums.IngredientCategory;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "ingredients")
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;        //  "Pomodorino ciliegino"

    @Column(name = "is_pantry_default")
    @Builder.Default
    private boolean isPantryDefault = false;

    @Column(name = "slug", unique = true)
    private String slug;      //  "pomodorino-ciliegino"

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private IngredientCategory category;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // senza questo Jackson stripperebbe l'"is" e serializzerebbe "pantryDefault", ma il frontend vuole "isPantryDefault"
    @JsonProperty("isPantryDefault")
    public boolean isPantryDefault() {
        return isPantryDefault;
    }

}
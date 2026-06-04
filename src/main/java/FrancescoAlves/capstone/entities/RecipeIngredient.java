package FrancescoAlves.capstone.entities;

import FrancescoAlves.capstone.enums.IngredientUnit;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "recipe_ingredients",
        uniqueConstraints = @UniqueConstraint(columnNames = {"recipe_id", "ingredient_id"}))
public class RecipeIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    private double quantity;

    @Column
    @Enumerated(EnumType.STRING)
    private IngredientUnit unit;

    @Column
    private String notes;

    // se false lo svuotafrigo non lo conta nello score di match
    @Column(name = "is_essential")
    @Builder.Default
    private boolean isEssential = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


}
package FrancescoAlves.capstone.entities;


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
@Table(name = "ingredient_substitutions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"ingredients_id", "substitutions_id"}))
public class IngredientSubstitutions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ingrediente richiesto dalla ricetta, es. "burrata"
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredients_id", nullable = false)
    private Ingredient ingredient;

    // quello con cui lo sostituisci, es. "stracciatella"
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "substitutions_id", nullable = false)
    private Ingredient substitutions;

    @Column(nullable = false)
    private String note;


    // quanto vale lo scambio (1.0 perfetto, 0.5 al limite), lo SvuotaFrigo lo usa come peso nello scoring
    @Column(name = "compatibility_score", nullable = false)
    private double compatibilityScore;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

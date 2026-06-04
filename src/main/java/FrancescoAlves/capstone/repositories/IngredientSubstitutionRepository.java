package FrancescoAlves.capstone.repositories;

import FrancescoAlves.capstone.entities.IngredientSubstitutions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IngredientSubstitutionRepository extends JpaRepository<IngredientSubstitutions, Long> {

    // regola bidirezionale: matcho sia ingredient sia substitutions, cosi' una
    // sola riga A<->B copre entrambe le direzioni. Il service capisce qual e' l'altro lato.
    @Query("""
            SELECT s FROM IngredientSubstitutions s
            WHERE (s.ingredient.id = :ingredientId OR s.substitutions.id = :ingredientId)
                AND s.compatibilityScore >= :minScore
            ORDER BY s.compatibilityScore DESC
            """)
    List<IngredientSubstitutions> findValidSubstitutes(@Param("ingredientId") Long ingredientId,
                                                       @Param("minScore") double minScore);

    // evita doppioni: c'e' gia' una regola tra i due, in una delle due direzioni?
    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
            FROM IngredientSubstitutions s
            WHERE (s.ingredient.id = :a AND s.substitutions.id = :b)
               OR (s.ingredient.id = :b AND s.substitutions.id = :a)
            """)
    boolean existsBetween(@Param("a") Long a, @Param("b") Long b);
}
package FrancescoAlves.capstone.repositories;

import FrancescoAlves.capstone.entities.RecipeIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {
}

package FrancescoAlves.capstone.repositories;

import FrancescoAlves.capstone.entities.IngredientAlias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IngredientAliasRepository extends JpaRepository<IngredientAlias, Long> {

    List<IngredientAlias> findByIngredientId(Long ingredientId);
}

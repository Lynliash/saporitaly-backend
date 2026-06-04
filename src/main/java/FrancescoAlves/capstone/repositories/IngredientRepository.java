package FrancescoAlves.capstone.repositories;

import FrancescoAlves.capstone.entities.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {


    Optional<Ingredient> findBySlug(String slug);

    Optional<Ingredient> findByNameIgnoreCase(String name);

    List<Ingredient> findByIsPantryDefaultTrue();


    // match su name o alias, per normalizzare l'input dell'utente
    @Query("""
            SELECT DISTINCT i FROM Ingredient i
            LEFT JOIN IngredientAlias a ON a.ingredient = i
            WHERE LOWER(i.slug) = LOWER(:searchTerm)
                OR LOWER(i.name) = LOWER(:searchTerm)
                OR LOWER(a.alias) = LOWER(:searchTerm)                                 
            """)
    Optional<Ingredient> findByNameOrAlias(@Param("searchTerm") String searchTerm);
}
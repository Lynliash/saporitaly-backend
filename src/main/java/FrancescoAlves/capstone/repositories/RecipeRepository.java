package FrancescoAlves.capstone.repositories;

import FrancescoAlves.capstone.entities.Recipe;
import FrancescoAlves.capstone.enums.RecipeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, UUID> {

    Page<Recipe> findByRegionId(Long regionId, Pageable pageable);

    Page<Recipe> findByRecipeType(RecipeType recipeType, Pageable pageable);

    long countByRegionId(Long regionId);

    // ricette che contengono un dato ingrediente
    @Query("""
                        SELECT r FROM Recipe r
                        JOIN r.recipeIngredients ri
                        WHERE ri.ingredient.id = :ingredientId
            """)
    Page<Recipe> findByIngredientId(@Param("ingredientId") Long ingredientId, Pageable pageable);

    // come IngredientRepository.findByNameOrAlias ma applicato in JOIN sulle ricette
    @Query("""
            SELECT DISTINCT r FROM Recipe r
            JOIN r.recipeIngredients ri
            JOIN  ri.ingredient i
            LEFT JOIN IngredientAlias a ON a.ingredient = i
            WHERE LOWER(i.slug) = LOWER(:searchTerm)
                OR LOWER(i.name) = LOWER(:searchTerm)
                OR LOWER(a.alias) = LOWER(:searchTerm)
            """)
    Page<Recipe> findByIngredientNameOrAlias(@Param("searchTerm") String searchTerm, Pageable pageable);

    // pre-filtro dello Svuota-frigo: ricette con almeno 1 ingrediente essenziale in
    // dispensa, o con un sostituto valido (score >= minScore). Lo scoring vero lo fa Java.
    // Le JOIN FETCH caricano ingredienti e regione in un colpo, cosi' il loop di scoring
    // non scatena N+1. minScore = 2.0 spegne il ramo sostituti (score max e' 1.0).
    @Query("""
            SELECT DISTINCT r FROM Recipe r
            LEFT JOIN FETCH r.recipeIngredients ri
            LEFT JOIN FETCH ri.ingredient
            LEFT JOIN FETCH r.region
            WHERE r.id IN (
                SELECT r2.id FROM Recipe r2
                JOIN r2.recipeIngredients ri2
                WHERE ri2.isEssential = true
                  AND (ri2.ingredient.id IN :pantryIngredientsId
                       OR EXISTS (SELECT s FROM IngredientSubstitutions s
                                  WHERE ((s.ingredient = ri2.ingredient AND s.substitutions.id IN :pantryIngredientsId)
                                       OR (s.substitutions = ri2.ingredient AND s.ingredient.id IN :pantryIngredientsId))
                                    AND s.compatibilityScore >= :minScore))
            )
            """)
    List<Recipe> findCandidatesForPantry(@Param("pantryIngredientsId") Set<Long> pantryIngredientsId,
                                         @Param("minScore") double minScore);

    // per validare il delete di un ingrediente
    @Query("""
            SELECT CASE WHEN COUNT(ri) > 0 THEN true ELSE false END
            FROM RecipeIngredient ri
            WHERE ri.ingredient.id = :ingredientId
            """)
    boolean isIngredientUsedInAnyRecipe(@Param("ingredientId") Long ingredientId);

}
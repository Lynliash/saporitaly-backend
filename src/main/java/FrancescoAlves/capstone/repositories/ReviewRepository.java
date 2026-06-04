package FrancescoAlves.capstone.repositories;

import FrancescoAlves.capstone.entities.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Page<Review> findByRecipeId(UUID recipeId, Pageable pageable);

    // contro i doppi voti, in aggiunta alla UNIQUE su DB
    Optional<Review> findByRecipeIdAndUserId(UUID recipeId, UUID userId);

    // per ricalcolare l'average_rating denormalizzato
    @Query("""
            SELECT AVG(r.rating), COUNT(r)
            FROM Review r
            WHERE r.recipe.id = :recipeId
            """)
    Object[] computeStatsForRecipe(@Param("recipeId") UUID recipeId);

    // serve a RecipeService.delete(): vanno tolte prima della ricetta o salta la FK
    @Modifying
    @Query("DELETE FROM Review r WHERE r.recipe.id = :recipeId")
    void deleteByRecipeId(@Param("recipeId") UUID recipeId);

    long countByRecipeId(UUID recipeId);

}

package FrancescoAlves.capstone.services;

import FrancescoAlves.capstone.payloads.CreateReviewRequest;
import FrancescoAlves.capstone.payloads.ReviewDto;
import FrancescoAlves.capstone.entities.Recipe;
import FrancescoAlves.capstone.entities.Review;
import FrancescoAlves.capstone.entities.User;
import FrancescoAlves.capstone.exceptions.NotFoundException;
import FrancescoAlves.capstone.exceptions.UnauthorizedException;
import FrancescoAlves.capstone.exceptions.ValidationException;
import FrancescoAlves.capstone.repositories.RecipeRepository;
import FrancescoAlves.capstone.repositories.ReviewRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeService recipeService;
    private final UserService userService;

    // crea la review e aggiorna i campi denormalizzati su Recipe, tutto in transazione
    @Transactional
    public ReviewDto createReview(UUID recipeId, UUID userId, CreateReviewRequest req) {
        Recipe recipe = recipeService.getEntityById(recipeId);
        User user = userService.findById(userId);

        // c'e' gia' UNIQUE(recipe_id, user_id) a DB, ma controllando qui do un errore chiaro
        // invece della constraint violation cruda
        reviewRepository.findByRecipeIdAndUserId(recipeId, userId)
                .ifPresent(existing -> {
                    throw new ValidationException("Hai gia' recensito questa ricetta. Usa PUT per modificare.");
                });

        Review review = Review.builder()
                .recipe(recipe)
                .user(user)
                .rating(req.getRating())
                .comment(req.getComment())
                .build();
        review = reviewRepository.save(review);

        recalculateRecipeStats(recipe);

        log.info("Review {} creata per recipe {} da user {}", review.getId(), recipeId, userId);
        return toDto(review);
    }

    // solo l'autore puo' modificare la propria review
    @Transactional
    public ReviewDto updateReview(UUID reviewId, UUID userId, CreateReviewRequest req) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review", reviewId));

        // Security ha verificato che e' loggato, qui verifico che sia il proprietario
        if (!review.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Non puoi modificare la review di un altro utente");
        }

        review.setRating(req.getRating());
        review.setComment(req.getComment());
        review = reviewRepository.save(review);

        recalculateRecipeStats(review.getRecipe());
        return toDto(review);
    }

    @Transactional
    public void deleteReview(UUID reviewId, UUID userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review", reviewId));

        if (!review.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Non puoi cancellare la review di un altro utente");
        }

        Recipe recipe = review.getRecipe();
        reviewRepository.delete(review);
        recalculateRecipeStats(recipe);
    }

    public Page<ReviewDto> findByRecipe(UUID recipeId, Pageable pageable) {
        return reviewRepository.findByRecipeId(recipeId, pageable).map(this::toDto);
    }

    // la review dell'utente loggato sulla ricetta, null se non l'ha ancora fatta.
    // serve al frontend per sapere se ho gia' votato e mostrare "modifica"
    public ReviewDto findMyReview(UUID recipeId, UUID userId) {
        return reviewRepository.findByRecipeIdAndUserId(recipeId, userId)
                .map(this::toDto)
                .orElse(null);
    }

    // ricalcola AVG e COUNT con una sola query, ad ogni create/update/delete di review
    private void recalculateRecipeStats(Recipe recipe) {
        Object[] stats = reviewRepository.computeStatsForRecipe(recipe.getId());
        // ritorna [Double avg, Long count]; con un solo risultato JPA a volte
        // lo wrappa in un Object[] di 2 elementi, a volte no
        Object[] row = (stats.length == 2 && !(stats[0] instanceof Object[])) ? stats : (Object[]) stats[0];

        Double avg = row[0] != null ? ((Number) row[0]).doubleValue() : 0.0;
        Integer count = row[1] != null ? ((Number) row[1]).intValue() : 0;

        recipe.setAverageRating(Math.round(avg * 100.0) / 100.0); // 2 decimali
        recipe.setReviewCount(count);
        recipeRepository.save(recipe);
    }

    private ReviewDto toDto(Review r) {
        return ReviewDto.builder()
                .id(r.getId())
                .recipeId(r.getRecipe().getId())
                .userId(r.getUser().getId())
                .username(r.getUser().getUsername())
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build();
    }
}

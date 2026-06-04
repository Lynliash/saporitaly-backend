package FrancescoAlves.capstone.controllers;

import FrancescoAlves.capstone.payloads.*;
import FrancescoAlves.capstone.security.UserDetailsImpl;
import FrancescoAlves.capstone.services.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/by-recipe/{recipeId}")
    public Page<ReviewDto> getByRecipe(@PathVariable UUID recipeId, Pageable pageable) {
        return reviewService.findByRecipe(recipeId, pageable);
    }

    // la mia review su questa ricetta: 200 + body se l'ho gia' votata, 204 se no.
    // cosi' il client sceglie tra "lascia recensione" e "modifica la tua"
    @GetMapping("/me/{recipeId}")
    public ResponseEntity<ReviewDto> myReview(
            @PathVariable UUID recipeId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        ReviewDto mine = reviewService.findMyReview(recipeId, currentUser.getUser().getId());
        return mine != null ? ResponseEntity.ok(mine) : ResponseEntity.noContent().build();
    }

    @PostMapping("/recipe/{recipeId}")
    public ResponseEntity<ReviewDto> create(
            @PathVariable UUID recipeId,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @Valid @RequestBody CreateReviewRequest req) {
        ReviewDto created = reviewService.createReview(recipeId, currentUser.getUser().getId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // solo l'autore puo' modificarla, check nel service
    @PutMapping("/{reviewId}")
    public ReviewDto update(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @Valid @RequestBody CreateReviewRequest req) {
        return reviewService.updateReview(reviewId, currentUser.getUser().getId(), req);
    }

    // solo l'autore, check nel service
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        reviewService.deleteReview(reviewId, currentUser.getUser().getId());
        return ResponseEntity.noContent().build();
    }
}
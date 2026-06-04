package FrancescoAlves.capstone.entities;

import FrancescoAlves.capstone.enums.Difficulty;
import FrancescoAlves.capstone.enums.RecipeType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "recipes")
public class Recipe {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private Difficulty difficulty;

    @Column(name = "prep_time_minutes", nullable = false)
    private int prepTimeMinutes;

    @Column(name = "cook_time_minutes", nullable = false)
    private int cookTimeMinutes;

    @Column
    private int servings;

    // TEXT perché gli URL delle immagini possono sforare i 255 char
    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;


    // denormalizzati, aggiornati a ogni save/delete di Review così non ricalcolo l'average a ogni lista
    @Column(name = "average_rating", nullable = false)
    @Builder.Default
    private double averageRating = 0.0;

    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private int reviewCount = 0;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    @Builder.Default
    private RecipeType recipeType = RecipeType.TRADITIONAL;


    // self reference per le REVISITATION, punta alla TRADITIONAL. nullable perché TRADITIONAL/PERSONAL non ce l'hanno
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_recipe_id")
    private Recipe originalRecipe;


    // per le TRADITIONAL è la regione di appartenenza, per PERSONAL/REVISITATION è opzionale
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;


    // null per le TRADITIONAL: le crea l'admin e non lo esponiamo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;


    // cascade + orphanRemoval: cancellando la ricetta spariscono anche step e ingredienti
    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RecipeStep> recipeSteps = new ArrayList<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RecipeIngredient> recipeIngredients = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
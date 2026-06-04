package FrancescoAlves.capstone.runners;

import FrancescoAlves.capstone.entities.*;
import FrancescoAlves.capstone.enums.*;
import FrancescoAlves.capstone.repositories.*;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Popola il DB al primo avvio: se la tabella users e' vuota seeda tutto, altrimenti skip.
 * Per ripartire da zero svuota il DB (o ddl-auto create-drop) e riavvia.
 *
 * Utenti qui nel codice, il resto del catalogo in resources/seed-data.json cosi'
 * si aggiorna senza ricompilare. Gli ingredienti non sono elencati a mano: li
 * ricavo dalle ricette (+ i pantry default), cosi' non manca mai un referenziato.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeederRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RegionRepository regionRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientAliasRepository aliasRepository;
    private final IngredientSubstitutionRepository substitutionRepository;
    private final RecipeRepository recipeRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    // tutto in una transazione: se fallisce, rollback completo e niente stato a meta'
    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("DB gia' popolato, skip seeding");
            return;
        }
        log.info("Inizio seeding del DB...");

        seedUsers();

        SeedData data = loadSeedData();
        if (data == null) {
            log.warn("seed-data.json non trovato o illeggibile: seedate solo le utenze.");
            return;
        }

        Map<String, Region> regions = seedRegions(data);
        Map<String, Ingredient> ingredients = seedIngredients(data);
        seedAliases(data, ingredients);
        seedSubstitutions(data, ingredients);
        seedRecipes(data, regions, ingredients);

        log.info("Seeding completato.");
    }

    private void seedUsers() {
        // demo BASE per provare login/registrazione/review
        userRepository.save(User.builder()
                .email("demo@saporitaly.it")
                .username("demo")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(UserRole.BASE)
                .build());

        // admin@saporitaly.it / admin123. L'admin si crea solo da qui: la
        // registrazione forza role=BASE (vedi AuthService su /auth/register).
        userRepository.save(User.builder()
                .email("admin@saporitaly.it")
                .username("admin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .role(UserRole.ADMIN)
                .build());

        log.info("Seeded 2 utenti: demo (BASE) e admin (ADMIN)");
    }

    private SeedData loadSeedData() {
        try (InputStream is = new ClassPathResource("seed-data.json").getInputStream()) {
            return objectMapper.readValue(is, SeedData.class);
        } catch (Exception e) {
            log.warn("Impossibile leggere seed-data.json: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Region> seedRegions(SeedData data) {
        List<Region> regions = new ArrayList<>();
        for (RegionSeed r : nullSafe(data.regions)) {
            regions.add(Region.builder().name(r.name).code(r.code).description(r.description).build());
        }
        regionRepository.saveAll(regions);
        Map<String, Region> map = new HashMap<>();
        regions.forEach(r -> map.put(r.getCode(), r));
        log.info("Seeded {} regioni", regions.size());
        return map;
    }

    // ingredienti = unione di quelli usati nelle ricette (a parita' di nome vince
    // il primo) piu' i pantry default
    private Map<String, Ingredient> seedIngredients(SeedData data) {
        // chiave = nome normalizzato lowercase
        LinkedHashMap<String, Ingredient> acc = new LinkedHashMap<>();
        Set<String> usedSlugs = new HashSet<>();

        Set<String> pantryDefaults = new HashSet<>();
        for (String name : nullSafe(data.pantryDefaults)) {
            pantryDefaults.add(key(name));
        }

        // pantry default per primi, sempre categoria PANTRY e flag true
        for (String name : nullSafe(data.pantryDefaults)) {
            register(acc, usedSlugs, name.trim(), IngredientCategory.PANTRY, true);
        }

        // poi tutti quelli citati dalle ricette
        for (RecipeSeed r : nullSafe(data.recipes)) {
            for (LineSeed l : nullSafe(r.ingredients)) {
                if (l.name == null || l.name.isBlank())
                    continue;
                boolean isDefault = pantryDefaults.contains(key(l.name));
                IngredientCategory cat = isDefault ? IngredientCategory.PANTRY
                        : (l.category != null ? l.category : IngredientCategory.OTHER);
                register(acc, usedSlugs, l.name.trim(), cat, isDefault);
            }
        }

        ingredientRepository.saveAll(acc.values());
        log.info("Seeded {} ingredienti (ricavati dalle ricette + pantry default)", acc.size());
        return acc; // chiave normalizzata, riusata dalle ricette
    }

    // aggiunge l'ingrediente se non c'e' gia' (per nome normalizzato)
    private void register(LinkedHashMap<String, Ingredient> acc, Set<String> usedSlugs,
            String name, IngredientCategory cat, boolean isDefault) {
        String k = key(name);
        if (acc.containsKey(k))
            return;
        String slug = uniqueSlug(slugify(name), usedSlugs);
        acc.put(k, Ingredient.builder()
                .name(name).slug(slug).category(cat).isPantryDefault(isDefault).build());
    }

    private void seedAliases(SeedData data, Map<String, Ingredient> ing) {
        Set<String> takenAliases = new HashSet<>();
        List<IngredientAlias> aliases = new ArrayList<>();
        for (AliasSeed a : nullSafe(data.aliases)) {
            if (a.alias == null || a.ingredient == null)
                continue;
            String aliasText = a.alias.trim().toLowerCase();
            Ingredient target = ing.get(key(a.ingredient));
            if (aliasText.isBlank() || target == null)
                continue;
            // alias unique e diverso dai nomi/slug esistenti
            if (!takenAliases.add(aliasText) || ing.containsKey(aliasText))
                continue;
            aliases.add(IngredientAlias.builder().alias(aliasText).ingredient(target).build());
        }
        aliasRepository.saveAll(aliases);
        log.info("Seeded {} alias", aliases.size());
    }

    private void seedSubstitutions(SeedData data, Map<String, Ingredient> ing) {
        Set<String> pairs = new HashSet<>();
        List<IngredientSubstitutions> subs = new ArrayList<>();
        for (SubSeed s : nullSafe(data.substitutions)) {
            if (s.a == null || s.b == null)
                continue;
            Ingredient a = ing.get(key(s.a));
            Ingredient b = ing.get(key(s.b));
            if (a == null || b == null || a == b)
                continue;
            // dedup bidirezionale: una regola sola per coppia
            String pairKey = a.getId() == null
                    ? unorderedKey(key(s.a), key(s.b))
                    : unorderedKey(String.valueOf(a.getSlug()), String.valueOf(b.getSlug()));
            if (!pairs.add(pairKey))
                continue;
            subs.add(IngredientSubstitutions.builder()
                    .ingredient(a).substitutions(b)
                    .compatibilityScore(s.score).note(s.note)
                    .build());
        }
        substitutionRepository.saveAll(subs);
        log.info("Seeded {} sostituzioni", subs.size());
    }

    private void seedRecipes(SeedData data, Map<String, Region> reg, Map<String, Ingredient> ing) {
        List<Recipe> recipes = new ArrayList<>();
        int skipped = 0;
        for (RecipeSeed r : nullSafe(data.recipes)) {
            RecipeType type = parseType(r.type);
            Recipe recipe = Recipe.builder()
                    .title(r.title)
                    .description(r.description)
                    .difficulty(r.difficulty)
                    .prepTimeMinutes(r.prepTimeMinutes)
                    .cookTimeMinutes(r.cookTimeMinutes)
                    .servings(r.servings)
                    .imageUrl(r.imageUrl)
                    .region(r.regionCode != null ? reg.get(r.regionCode) : null)
                    .recipeType(type)
                    .build();

            boolean hasEssential = false;
            Set<String> seenInRecipe = new HashSet<>();
            for (LineSeed l : nullSafe(r.ingredients)) {
                Ingredient i = ing.get(key(l.name));
                if (i == null)
                    continue; // difensivo, non dovrebbe capitare
                // stesso ingrediente due volte romperebbe l'unique (recipe_id, ingredient_id): tengo la prima
                if (!seenInRecipe.add(key(l.name)))
                    continue;
                recipe.getRecipeIngredients().add(RecipeIngredient.builder()
                        .recipe(recipe).ingredient(i)
                        .quantity(l.quantity).unit(l.unit)
                        .isEssential(l.essential).notes(l.note)
                        .build());
                hasEssential = hasEssential || l.essential;
            }
            // se nessuno e' essenziale (dato sporco) forzo il primo
            if (!hasEssential && !recipe.getRecipeIngredients().isEmpty()) {
                recipe.getRecipeIngredients().iterator().next().setEssential(true);
            }
            if (recipe.getRecipeIngredients().isEmpty()) {
                skipped++;
                continue;
            }

            int order = 1;
            for (String stepDesc : nullSafe(r.steps)) {
                recipe.getRecipeSteps().add(RecipeStep.builder()
                        .recipe(recipe).stepOrder(order++).description(stepDesc).build());
            }
            recipes.add(recipe);
        }
        recipeRepository.saveAll(recipes);
        log.info("Seeded {} ricette ({} saltate per dati incompleti)", recipes.size(), skipped);
    }

    private static <T> List<T> nullSafe(List<T> l) {
        return l != null ? l : List.of();
    }

    private static String key(String name) {
        return name == null ? "" : name.trim().toLowerCase();
    }

    private static String unorderedKey(String a, String b) {
        return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
    }

    private static RecipeType parseType(String type) {
        if (type == null || type.isBlank())
            return RecipeType.TRADITIONAL;
        try {
            return RecipeType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return RecipeType.TRADITIONAL;
        }
    }

    private static String uniqueSlug(String base, Set<String> used) {
        String slug = base.isBlank() ? "ingrediente" : base;
        String candidate = slug;
        int n = 2;
        while (!used.add(candidate)) {
            candidate = slug + "-" + n++;
        }
        return candidate;
    }

    private static String slugify(String name) {
        return name.trim().toLowerCase()
                .replace("'", "-")
                .replaceAll("[\\s+]", "-")
                .replaceAll("[^a-z0-9\\-]", "")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    // strutture di parsing del seed-data.json (campi public per Jackson)

    static class SeedData {
        public List<RegionSeed> regions;
        public List<String> pantryDefaults;
        public List<AliasSeed> aliases;
        public List<SubSeed> substitutions;
        public List<RecipeSeed> recipes;
    }

    static class RegionSeed {
        public String code;
        public String name;
        public String description;
    }

    static class AliasSeed {
        public String alias;
        public String ingredient;
    }

    static class SubSeed {
        public String a;
        public String b;
        public double score;
        public String note;
    }

    static class RecipeSeed {
        public String title;
        public String description;
        public Difficulty difficulty;
        public int prepTimeMinutes;
        public int cookTimeMinutes;
        public int servings;
        public String type;
        public String regionCode;
        public String imageUrl;
        public List<LineSeed> ingredients;
        public List<String> steps;
    }

    static class LineSeed {
        public String name;
        public IngredientCategory category;
        public double quantity;
        public IngredientUnit unit;
        public boolean essential;
        public String note;
    }
}

package restaurant.recommendation.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import restaurant.recommendation.model.*;

class RecommendationEngineTest {
    private RecommendationEngine engine;
    private MenuCategory veg, nonVeg, desserts;

    @BeforeEach
    void setUp() {
        engine = new RecommendationEngine();
        engine.setInventory(new Inventory(10)); // everything in stock unless a test says otherwise
        veg = new MenuCategory("C1", "Vegetarian Food", "");
        nonVeg = new MenuCategory("C2", "Non-Vegetarian Food", "");
        desserts = new MenuCategory("C3", "Desserts", "");
        add("V1", "Veg Burger", veg, 50, 4.2, List.of("bread", "cheese"), "vegetarian");
        add("V2", "Vada Pav", veg, 25, 4.5, List.of("bread", "potato"), "vegetarian", "spicy");
        add("V3", "Salad", veg, 40, 4.0, List.of("lettuce", "tomato"), "vegan", "healthy");
        add("N1", "Chicken Roll", nonVeg, 120, 4.8, List.of("chicken", "bread"), "non-vegetarian", "spicy");
        add("N2", "Egg Fried Rice", nonVeg, 80, 4.4, List.of("rice", "egg"), "non-vegetarian");
        add("D1", "Brownie", desserts, 50, 4.6, List.of("flour", "egg", "chocolate"), "vegetarian", "sweet");
        add("D2", "Fruit Bowl", desserts, 60, 4.1, List.of("fruit"), "vegan", "sweet");
    }

    private void add(String id, String name, MenuCategory cat, double price, double rating,
                     List<String> ingredients, String... tags) {
        MenuItem item = new MenuItem(id, name, ingredients, cat, price);
        item.setTags(Arrays.asList(tags));
        item.updateRating(rating);
        engine.addMenuItem(item);
    }

    private UserPreferences profile(String userId) {
        UserPreferences p = new UserPreferences(userId);
        p.setPriceRange(0, 200);
        engine.getUserProfileManager().createUserProfile(userId, p);
        return p;
    }

    private List<String> ids(String userId, int max) {
        return engine.generateRecommendations(userId, null, max).getRecommendationList().stream()
                .map(MenuItem::getItemId).collect(Collectors.toList());
    }

    private void giveOrderHistory(String userId) {
        engine.getUserProfileManager().addOrderToHistory(userId, new Order("O1", userId, List.of("V1"), 50));
    }

    // ---- dietary constraints are hard rules for every kind of user ----

    @Test
    void newVegetarianUserIsNeverShownMeat() {
        profile("u").setVegPreference(true);
        List<String> got = ids("u", 10);
        assertFalse(got.isEmpty());
        assertTrue(Collections.disjoint(got, List.of("N1", "N2")), got.toString());
    }

    @Test
    void returningVegetarianUserIsNeverShownMeat() {
        profile("u").setVegPreference(true);
        giveOrderHistory("u");
        assertTrue(Collections.disjoint(ids("u", 10), List.of("N1", "N2")));
    }

    @Test
    void veganUserOnlyGetsVeganItems() {
        profile("u").setVeganPreference(true);
        assertEquals(Set.of("V3", "D2"), new HashSet<>(ids("u", 10)));
    }

    @Test
    void allergensAreExcludedForNewAndReturningUsers() {
        profile("u").addAllergen("egg");
        assertTrue(Collections.disjoint(ids("u", 10), List.of("N2", "D1")));
        giveOrderHistory("u");
        assertTrue(Collections.disjoint(ids("u", 10), List.of("N2", "D1")));
    }

    @Test
    void outOfStockItemsAreNotRecommended() {
        profile("u");
        engine.getInventory().setStock("N1", 0);
        assertFalse(ids("u", 10).contains("N1"));
    }

    // ---- ranking ----

    @Test
    void usersWithDifferentProfilesGetDifferentRecommendations() {
        UserPreferences sweet = profile("sweet");
        sweet.addPreferredTag("sweet");
        sweet.updateCategoryPreference("Desserts", 1.0);
        UserPreferences meat = profile("meat");
        meat.updateCategoryPreference("Non-Vegetarian Food", 1.0);
        meat.setSpicinessLevel(5);

        List<String> forSweet = ids("sweet", 2);
        List<String> forMeat = ids("meat", 2);
        assertEquals(Set.of("D1", "D2"), new HashSet<>(forSweet));
        assertEquals("N1", forMeat.get(0));
        assertNotEquals(forSweet, forMeat);
    }

    @Test
    void newUserWithProfileIsMatchedAgainstIt() {
        profile("u");
        assertEquals("Cold Start - Profile Match",
                engine.generateRecommendations("u", null, 3).getAlgorithmUsed());
    }

    @Test
    void unknownUserGetsHighestRatedItems() {
        RecommendationResult r = engine.generateRecommendations("stranger", null, 3);
        assertEquals("Cold Start - Popular Items", r.getAlgorithmUsed());
        assertEquals(List.of("N1", "D1", "V2"),
                r.getRecommendationList().stream().map(MenuItem::getItemId).collect(Collectors.toList()));
    }

    @Test
    void returningUserUsesTheConfiguredAlgorithm() {
        profile("u");
        giveOrderHistory("u");
        assertEquals("Hybrid", engine.generateRecommendations("u", null, 3).getAlgorithmUsed());
    }

    @Test
    void rankingDoesNotDependOnMenuInsertionOrder() {
        profile("u");
        List<String> first = ids("u", 7);

        RecommendationEngine reversed = new RecommendationEngine();
        reversed.setInventory(new Inventory(10));
        List<MenuItem> items = engine.getMenuItems();
        Collections.reverse(items);
        items.forEach(reversed::addMenuItem);
        UserPreferences p = new UserPreferences("u");
        p.setPriceRange(0, 200);
        reversed.getUserProfileManager().createUserProfile("u", p);
        List<String> second = reversed.generateRecommendations("u", null, 7).getRecommendationList().stream()
                .map(MenuItem::getItemId).collect(Collectors.toList());

        assertEquals(first, second);
    }

    @Test
    void resultSizeIsCappedAndNeverNegative() {
        profile("u");
        assertEquals(2, ids("u", 2).size());
        assertTrue(ids("u", 0).isEmpty());
        assertTrue(ids("u", -1).isEmpty());
        assertEquals(7, ids("u", 100).size());
    }

    @Test
    void everyRecommendationHasAnExplanationAndAConfidenceInRange() {
        profile("u");
        RecommendationResult r = engine.generateRecommendations("u", null, 5);
        for (MenuItem item : r.getRecommendationList()) {
            assertNotNull(r.getExplanation(item.getItemId()));
            double c = r.getConfidenceScore(item.getItemId());
            assertTrue(c >= 0.0 && c <= 1.0, "confidence " + c);
        }
    }

    // ---- metrics ----

    @Test
    void servingRecommendationsDoesNotInventRatings() {
        profile("u");
        ids("u", 3);
        ids("u", 3);
        Map<String, Double> m = engine.getPerformanceMetrics();
        assertEquals(2.0, m.get("total_recommendations"));
        assertEquals(0.0, m.get("feedback_count"));
        assertEquals(0.0, m.get("success_rate"));
        assertTrue(m.get("avg_confidence") > 0.0 && m.get("avg_confidence") <= 1.0);
    }

    @Test
    void ratingMetricsComeFromFeedbackOnly() {
        profile("u");
        feedback("u", "V1", 5.0);
        feedback("u", "V2", 2.0);
        Map<String, Double> m = engine.getPerformanceMetrics();
        assertEquals(2.0, m.get("feedback_count"));
        assertEquals(3.5, m.get("avg_rating"), 1e-9);
        assertEquals(0.5, m.get("success_rate"), 1e-9);
        assertEquals(0.0, m.get("total_recommendations"));
    }

    private void feedback(String userId, String itemId, double rating) {
        Map<String, Object> f = new HashMap<>();
        f.put("user_id", userId);
        f.put("item_id", itemId);
        f.put("rating", rating);
        engine.refineAlgorithms(f);
    }
}

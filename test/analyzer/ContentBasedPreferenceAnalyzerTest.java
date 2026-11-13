package restaurant.recommendation.analyzer;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import restaurant.recommendation.model.*;

class ContentBasedPreferenceAnalyzerTest {
    private final ContentBasedPreferenceAnalyzer analyzer = new ContentBasedPreferenceAnalyzer();
    private final MenuCategory mains = new MenuCategory("C1", "Mains", "");

    private MenuItem item(double price, String... tags) {
        MenuItem m = new MenuItem("I1", "Dish", List.of("x"), mains, price);
        m.setTags(Arrays.asList(tags));
        return m;
    }

    private UserPreferences prefs() {
        UserPreferences p = new UserPreferences("u");
        p.setPriceRange(0, 100);
        p.setSpicinessLevel(1);
        return p;
    }

    private double score(MenuItem m, UserPreferences p) {
        return analyzer.scoreItemsBasedOnPreferences(m, p);
    }

    @Test
    void weightsSumToOne() {
        assertEquals(1.0, ContentBasedPreferenceAnalyzer.W_PRICE + ContentBasedPreferenceAnalyzer.W_SPICE
                + ContentBasedPreferenceAnalyzer.W_CATEGORY + ContentBasedPreferenceAnalyzer.W_TAGS
                + ContentBasedPreferenceAnalyzer.W_DIET, 1e-9);
    }

    @Test
    void scoreStaysInUnitRangeAtTheExtremes() {
        UserPreferences best = prefs();
        best.setVegPreference(true);
        best.addPreferredTag("healthy");
        best.updateCategoryPreference("Mains", 1.0);
        assertEquals(1.0, score(item(50, "vegetarian", "healthy"), best), 1e-9);

        UserPreferences worst = prefs();
        worst.addPreferredTag("sweet");
        worst.updateCategoryPreference("Mains", -1.0);
        double low = score(item(500, "spicy"), worst);
        assertTrue(low >= 0.0 && low < 0.1, "score " + low);
    }

    @Test
    void anItemInBudgetBeatsTheSameItemOutOfBudget() {
        UserPreferences p = prefs();
        assertTrue(score(item(50), p) > score(item(500), p));
    }

    @Test
    void spiceLoversPreferSpicyAndMildEatersPreferMild() {
        UserPreferences hot = prefs();
        hot.setSpicinessLevel(5);
        assertTrue(score(item(50, "spicy"), hot) > score(item(50), hot));

        UserPreferences mild = prefs();
        mild.setSpicinessLevel(1);
        assertTrue(score(item(50), mild) > score(item(50, "spicy"), mild));
    }

    @Test
    void preferredTagAndCategoryRaiseTheScore() {
        UserPreferences plain = prefs();
        UserPreferences keen = prefs();
        keen.addPreferredTag("sweet");
        keen.updateCategoryPreference("Mains", 1.0);
        assertTrue(score(item(50, "sweet"), keen) > score(item(50, "sweet"), plain));
    }

    @Test
    void eatingMeatIsNotAPreferenceAgainstVegetarianFood() {
        UserPreferences omnivore = prefs();
        assertEquals(score(item(50, "vegetarian"), omnivore), score(item(50, "non-vegetarian"), omnivore), 1e-9);
    }

    @Test
    void explanationNamesTheReasons() {
        UserPreferences p = prefs();
        p.setVegPreference(true);
        p.addPreferredTag("healthy");
        String why = analyzer.getExplanation(item(50, "vegetarian", "healthy"), p);
        assertTrue(why.contains("vegetarian choice"), why);
        assertTrue(why.contains("within your budget"), why);
        assertTrue(why.contains("tagged healthy"), why);
        assertFalse(why.endsWith(", "), why);
    }
}

package restaurant.recommendation.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class MenuItemTest {
    private MenuItem item(String... ingredients) {
        return new MenuItem("I1", "Dish", Arrays.asList(ingredients), new MenuCategory("C1", "Mains", ""), 50);
    }

    @Test
    void allergenMatchIgnoresCaseAndPlural() {
        assertTrue(item("rice", "egg").hasAllergen("eggs"));
        assertTrue(item("rice", "Eggs").hasAllergen("EGG"));
        assertTrue(item("peanuts").hasAllergen("nut"));
        assertTrue(item("peanut butter").hasAllergen("peanuts"));
    }

    @Test
    void unrelatedIngredientsDoNotMatch() {
        assertFalse(item("rice", "chicken").hasAllergen("eggs"));
        assertFalse(item("rice").hasAllergen(""));
        assertFalse(item("rice").hasAllergen(null));
        assertFalse(item("rice", null).hasAllergen("egg"));
    }

    @Test
    void shortWordsEndingInSAreNotTrimmed() {
        // "gas" must not become "ga" and match "garlic"
        assertFalse(item("garlic").hasAllergen("gas"));
    }

    @Test
    void dietFlagsComeFromTags() {
        MenuItem vegan = item("lettuce");
        vegan.setTags(List.of("vegan"));
        assertTrue(vegan.isVegan());
        assertTrue(vegan.isVegetarian(), "vegan food is also vegetarian");
        MenuItem meat = item("chicken");
        meat.setTags(List.of("non-vegetarian"));
        assertFalse(meat.isVegetarian());
    }
}

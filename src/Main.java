package restaurant.recommendation;

import restaurant.recommendation.engine.*;
import restaurant.recommendation.model.*;
import restaurant.recommendation.analyzer.*;
import restaurant.recommendation.filter.*;
import restaurant.recommendation.tracker.*;

import java.time.LocalDateTime;
import java.util.*;


public class Main {
    public static void main(String[] args) {
        System.out.println("=== Ice & Spice Restaurant Recommendation System ===\n");
        System.out.println("Architecture: UML-compliant with Abstract Classes and Inheritance");

        RecommendationEngine engine = setupRecommendationSystem();

        setupIceSpiceMenuData(engine);

        demonstrateRecommendations(engine);

        System.out.println("\n=== Demo completed successfully ===");
    }

    private static RecommendationEngine setupRecommendationSystem() {
        System.out.println("\n1. Setting up Ice & Spice Recommendation System...");

        PreferenceAnalyzer contentAnalyzer = new ContentBasedPreferenceAnalyzer();
        PreferenceAnalyzer collabAnalyzer = new CollaborativePreferenceAnalyzer();
        FeedbackAnalyzer feedbackAnalyzer = new AdvancedFeedbackAnalyzer();

        UserProfileManager userManager = new UserProfileManager(feedbackAnalyzer);

        RecommendationEngine engine = new RecommendationEngine(contentAnalyzer, feedbackAnalyzer, userManager);

        Inventory inventory = new Inventory(5);
        engine.setInventory(inventory);

        System.out.println("   ✓ Abstract analyzers instantiated");
        System.out.println("   ✓ Composition relationships established");
        System.out.println("   ✓ Aggregation relationships configured");
        return engine;
    }

    private static void setupIceSpiceMenuData(RecommendationEngine engine) {
        System.out.println("\n2. Creating Ice & Spice menu with category hierarchy...");

        MenuCategory food = new MenuCategory("CAT000", "Food", "All food items");
        MenuCategory vegetarianFood = new MenuCategory("CAT001", "Vegetarian Food", "Vegetarian dishes");
        MenuCategory nonVegFood = new MenuCategory("CAT002", "Non-Vegetarian Food", "Non-vegetarian dishes");
        MenuCategory liveCounter = new MenuCategory("CAT003", "Live Counter", "Freshly prepared items");
        MenuCategory beverages = new MenuCategory("CAT004", "Beverages", "Drinks and beverages");
        MenuCategory desserts = new MenuCategory("CAT005", "Desserts", "Sweet treats");

        vegetarianFood.setParentCategory(food);
        nonVegFood.setParentCategory(food);
        food.addChildCategory(vegetarianFood);
        food.addChildCategory(nonVegFood);

        List<MenuItem> menuItems = createIceSpiceMenuItems(vegetarianFood, nonVegFood, liveCounter, beverages, desserts);

        for (MenuItem item : menuItems) {
            engine.addMenuItem(item);
        }

        setupInventoryTracking(engine, menuItems);

        createUserProfiles(engine);

        createPromotionalOffers(engine);

        System.out.println("   ✓ Menu hierarchy created: " + menuItems.size() + " items");
        System.out.println("   ✓ Category relationships established");
        System.out.println("   ✓ User profiles with preferences created");
    }

    private static List<MenuItem> createIceSpiceMenuItems(MenuCategory vegFood, MenuCategory nonVegFood, 
                                                         MenuCategory liveCounter, MenuCategory beverages,
                                                         MenuCategory desserts) {
        List<MenuItem> items = new ArrayList<>();

        MenuItem vegBurger = new MenuItem("VEG001", "Veg Burger", 
                Arrays.asList("bread", "vegetables", "cheese"), vegFood, 50.0);
        vegBurger.setTags(Arrays.asList("vegetarian"));
        vegBurger.updateRating(4.2);

        MenuItem vadaPav = new MenuItem("VEG004", "Vada Pav", 
                Arrays.asList("bread", "potato", "spices"), vegFood, 25.0);
        vadaPav.setTags(Arrays.asList("vegetarian", "spicy"));
        vadaPav.updateRating(4.5);

        MenuItem friedRice = new MenuItem("VEG003", "Fried Rice", 
                Arrays.asList("rice", "vegetables", "spices"), vegFood, 60.0);
        friedRice.setTags(Arrays.asList("vegetarian", "spicy"));
        friedRice.updateRating(4.3);

        MenuItem maggi = new MenuItem("VEG005", "Maggi", 
                Arrays.asList("noodles", "vegetables", "spices"), vegFood, 30.0);
        maggi.setTags(Arrays.asList("vegetarian"));
        maggi.updateRating(4.1);

        MenuItem paneerSandwich = new MenuItem("VEG007", "Paneer Cheese Sandwich", 
                Arrays.asList("bread", "paneer", "cheese"), vegFood, 70.0);
        paneerSandwich.setTags(Arrays.asList("vegetarian"));
        paneerSandwich.updateRating(4.4);

        MenuItem chickenBurger = new MenuItem("NONVEG001", "Chicken Burger", 
                Arrays.asList("bread", "chicken", "vegetables"), nonVegFood, 60.0);
        chickenBurger.setTags(Arrays.asList("non-vegetarian"));
        chickenBurger.updateRating(4.3);

        MenuItem shawarma = new MenuItem("NONVEG004", "Chicken Shawarma", 
                Arrays.asList("bread", "chicken", "sauce", "spices"), nonVegFood, 70.0);
        shawarma.setTags(Arrays.asList("non-vegetarian", "spicy"));
        shawarma.updateRating(4.6);

        MenuItem chickenFriedRice = new MenuItem("NONVEG005", "Chicken Fried Rice", 
                Arrays.asList("rice", "chicken", "vegetables", "spices"), nonVegFood, 80.0);
        chickenFriedRice.setTags(Arrays.asList("non-vegetarian", "spicy"));
        chickenFriedRice.updateRating(4.4);

        MenuItem pavBhaji = new MenuItem("LIVE003", "Pav Bhaji", 
                Arrays.asList("bread", "mixed_vegetables", "butter", "spices"), liveCounter, 60.0);
        pavBhaji.setTags(Arrays.asList("vegetarian", "spicy"));
        pavBhaji.updateRating(4.7);

        MenuItem chickenRoll = new MenuItem("LIVE005", "Chicken Special Roll", 
                Arrays.asList("bread", "chicken", "vegetables", "spices"), liveCounter, 120.0);
        chickenRoll.setTags(Arrays.asList("non-vegetarian", "spicy"));
        chickenRoll.updateRating(4.8);

        MenuItem milkshake = new MenuItem("DRINK001", "Milkshake", 
                Arrays.asList("milk", "flavoring"), beverages, 50.0);
        milkshake.setTags(Arrays.asList("vegetarian", "cold"));
        milkshake.updateRating(4.3);

        MenuItem tea = new MenuItem("DRINK004", "Tea", 
                Arrays.asList("tea_leaves", "milk", "sugar"), beverages, 15.0);
        tea.setTags(Arrays.asList("vegetarian", "hot"));
        tea.updateRating(4.0);

        MenuItem freshJuice = new MenuItem("DRINK003", "Fresh Juice", 
                Arrays.asList("fruits"), beverages, 40.0);
        freshJuice.setTags(Arrays.asList("vegan", "healthy"));
        freshJuice.updateRating(4.2);

        // Desserts
        MenuItem brownie = new MenuItem("DESSERT003", "Brownie", 
                Arrays.asList("chocolate", "flour", "butter"), desserts, 50.0);
        brownie.setTags(Arrays.asList("vegetarian", "sweet"));
        brownie.updateRating(4.6);

        MenuItem pastry = new MenuItem("DESSERT001", "Pastry", 
                Arrays.asList("flour", "cream", "sugar"), desserts, 80.0);
        pastry.setTags(Arrays.asList("vegetarian", "sweet"));
        pastry.updateRating(4.5);

        items.addAll(Arrays.asList(
            vegBurger, vadaPav, friedRice, maggi, paneerSandwich,
            chickenBurger, shawarma, chickenFriedRice,
            pavBhaji, chickenRoll,
            milkshake, tea, freshJuice,
            brownie, pastry
        ));

        return items;
    }

    private static void setupInventoryTracking(RecommendationEngine engine, List<MenuItem> menuItems) {
        Inventory inventory = engine.getInventory();

        // Set realistic stock levels for demonstration
        inventory.setStock("VEG001", 20);  // Veg Burger
        inventory.setStock("VEG004", 25);  // Vada Pav - popular
        inventory.setStock("VEG003", 15);  // Fried Rice
        inventory.setStock("VEG005", 18);  // Maggi
        inventory.setStock("VEG007", 8);   // Paneer Sandwich
        inventory.setStock("NONVEG001", 12); // Chicken Burger
        inventory.setStock("NONVEG004", 10); // Shawarma
        inventory.setStock("NONVEG005", 8);  // Chicken Fried Rice
        inventory.setStock("LIVE003", 20);   // Pav Bhaji - very popular
        inventory.setStock("LIVE005", 5);    // Special Roll - premium
        inventory.setStock("DRINK001", 25);  // Milkshake
        inventory.setStock("DRINK004", 30);  // Tea - always available
        inventory.setStock("DRINK003", 15);  // Fresh Juice
        inventory.setStock("DESSERT003", 12); // Brownie
        inventory.setStock("DESSERT001", 8);  // Pastry
    }

    private static void createUserProfiles(RecommendationEngine engine) {
        // User 1: Budget-conscious student
        UserPreferences student = new UserPreferences("STUDENT001");
        student.setSpicinessLevel(4);
        student.setPriceRange(15.0, 80.0);
        student.addFavouriteCuisine("Indian");

        // User 2: Health-conscious vegetarian
        UserPreferences healthUser = new UserPreferences("HEALTH001");
        healthUser.setVegPreference(true);
        healthUser.setSpicinessLevel(2);
        healthUser.setPriceRange(20.0, 100.0);
        healthUser.addAllergen("eggs");

        // User 3: Sweet tooth
        UserPreferences sweetUser = new UserPreferences("SWEET001");
        sweetUser.setSpicinessLevel(1);
        sweetUser.setPriceRange(15.0, 120.0);

        // User 4: Meat lover
        UserPreferences meatLover = new UserPreferences("MEAT001");
        meatLover.setVegPreference(false);
        meatLover.setSpicinessLevel(3);
        meatLover.setPriceRange(40.0, 150.0);

        // Load profiles into the system
        engine.retrieveUserProfile("STUDENT001");
        engine.retrieveUserProfile("HEALTH001");
        engine.retrieveUserProfile("SWEET001");  
        engine.retrieveUserProfile("MEAT001");
    }

    private static void createPromotionalOffers(RecommendationEngine engine) {
        List<Offer> offers = new ArrayList<>();

        // Student discount
        Offer studentDiscount = new Offer("ICE001", "Student Special", 15.0);
        studentDiscount.setDescription("15% off on items under ₹50");
        studentDiscount.setValidUntil(LocalDateTime.now().plusDays(30));
        offers.add(studentDiscount);

        // Combo deal
        Offer comboDeal = new Offer("ICE002", "Food + Drink Combo", 10.0);
        comboDeal.setDescription("10% off when you order food with drinks");
        offers.add(comboDeal);

        engine.coordinateWithOffers(offers);
    }

    private static void demonstrateRecommendations(RecommendationEngine engine) {
        System.out.println("\n3. Demonstrating UML-compliant recommendations...\n");

        ContextualFactor lunchContext = new ContextualFactor();
        lunchContext.setTimeOfDay("afternoon");
        lunchContext.setTemperature(28.0);

        // Demo different user personas
        String[] userIds = {"STUDENT001", "HEALTH001", "SWEET001", "MEAT001"};
        String[] descriptions = {
            "Budget-Conscious Student", 
            "Health-Conscious Vegetarian",
            "Sweet Tooth User",
            "Non-Vegetarian Food Lover"
        };

        for (int i = 0; i < userIds.length; i++) {
            System.out.println("--- " + descriptions[i] + " (" + userIds[i] + ") ---");

            try {
                RecommendationResult result = engine.generateRecommendations(
                    userIds[i], lunchContext, 3);

                System.out.println("Algorithm: " + result.getAlgorithmUsed());
                System.out.println("Avg Confidence: " + 
                    String.format("%.2f", result.getAverageConfidence()));

                if (result.getRecommendationList().isEmpty()) {
                    System.out.println("  No specific recommendations available");
                } else {
                    for (int j = 0; j < result.getRecommendationList().size(); j++) {
                        MenuItem item = result.getRecommendationList().get(j);
                        String explanation = result.getExplanation(item.getItemId());
                        double confidence = result.getConfidenceScore(item.getItemId());

                        System.out.println(String.format("  %d. %s (₹%.0f) - Rating: %.1f - Confidence: %.2f", 
                            j+1, item.getName(), item.getPrice(), item.getAverageRating(), confidence));
                        System.out.println("     " + explanation);
                    }
                }
            } catch (Exception e) {
                System.out.println("  Error: " + e.getMessage());
            }

            System.out.println();
        }

        // Demonstrate feedback processing
        System.out.println("--- Feedback Processing Demo ---");
        Map<String, Object> feedback = new HashMap<>();
        feedback.put("user_id", "STUDENT001");
        feedback.put("item_id", "VEG004"); // Vada Pav
        feedback.put("rating", 5.0);
        feedback.put("comment", "Perfect student food! Cheap and delicious.");

        engine.refineAlgorithms(feedback);
        System.out.println("✓ Processed positive feedback for Vada Pav");

        System.out.println("\n🎯 UML Compliance Demonstrated:");
        System.out.println("   ✓ Abstract classes: PreferenceAnalyzer, FeedbackAnalyzer");
        System.out.println("   ✓ Concrete implementations with polymorphism");
        System.out.println("   ✓ Composition: Engine -> Analyzer components");
        System.out.println("   ✓ Aggregation: Engine -> UserProfileManager, Inventory");
        System.out.println("   ✓ Association: Various data relationships");
        System.out.println("   ✓ Inheritance: MenuItem implements Comparable");
        System.out.println("   ✓ Reflexive: MenuCategory parent-child relationships");
    }
}
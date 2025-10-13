package restaurant.recommendation.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ContextualFactor class as per UML diagram
 * Contains environmental and session-specific data
 */
public class ContextualFactor {
    // UML specified attributes
    private String season;
    private String timeOfDay;
    private String weather;

    // Additional contextual attributes
    private LocalDateTime currentTime;
    private boolean isWeekend;
    private double temperature;
    private String location;
    private String deviceType;
    private int groupSize;

    // Constructor
    public ContextualFactor() {
        this.currentTime = LocalDateTime.now();
        determineTimeOfDay();
        determineSeason();
        determineWeekend();
    }

    public ContextualFactor(String timeOfDay, String weather, double temperature) {
        this();
        this.timeOfDay = timeOfDay;
        this.weather = weather;
        this.temperature = temperature;
    }

    // Auto-determination methods
    private void determineTimeOfDay() {
        if (currentTime == null) {
            this.timeOfDay = "unknown";
            return;
        }

        int hour = currentTime.getHour();
        if (hour >= 6 && hour < 12) {
            this.timeOfDay = "morning";
        } else if (hour >= 12 && hour < 17) {
            this.timeOfDay = "afternoon";
        } else if (hour >= 17 && hour < 22) {
            this.timeOfDay = "evening";
        } else {
            this.timeOfDay = "night";
        }
    }

    private void determineSeason() {
        if (currentTime == null) {
            this.season = "unknown";
            return;
        }

        int month = currentTime.getMonthValue();
        if (month >= 3 && month <= 5) {
            this.season = "spring";
        } else if (month >= 6 && month <= 8) {
            this.season = "summer";
        } else if (month >= 9 && month <= 11) {
            this.season = "autumn";
        } else {
            this.season = "winter";
        }
    }

    private void determineWeekend() {
        if (currentTime == null) {
            this.isWeekend = false;
            return;
        }

        int dayOfWeek = currentTime.getDayOfWeek().getValue();
        this.isWeekend = (dayOfWeek == 6 || dayOfWeek == 7); // Saturday or Sunday
    }

    // Contextual condition methods
    public boolean isHotWeather() {
        return temperature > 25.0;
    }

    public boolean isColdWeather() {
        return temperature < 15.0;
    }

    public boolean isMildWeather() {
        return temperature >= 15.0 && temperature <= 25.0;
    }

    public boolean isBreakfastTime() {
        return "morning".equals(timeOfDay);
    }

    public boolean isLunchTime() {
        return "afternoon".equals(timeOfDay);
    }

    public boolean isDinnerTime() {
        return "evening".equals(timeOfDay);
    }

    public boolean isLateNight() {
        return "night".equals(timeOfDay);
    }

    public boolean isSummerSeason() {
        return "summer".equals(season);
    }

    public boolean isWinterSeason() {
        return "winter".equals(season);
    }

    public boolean isRainyWeather() {
        return weather != null && (weather.toLowerCase().contains("rain") || 
                                 weather.toLowerCase().contains("storm") ||
                                 weather.toLowerCase().contains("drizzle"));
    }

    public boolean isSunnyWeather() {
        return weather != null && (weather.toLowerCase().contains("sunny") || 
                                 weather.toLowerCase().contains("clear"));
    }

    public boolean isGroupOrder() {
        return groupSize > 1;
    }

    public boolean isSoloOrder() {
        return groupSize <= 1;
    }

    // Recommendation influence methods
    public List<String> getRecommendedTags() {
        List<String> tags = new ArrayList<>();

        // Weather-based recommendations
        if (isHotWeather()) {
            tags.addAll(Arrays.asList("cold", "refreshing", "light", "salad"));
        } else if (isColdWeather()) {
            tags.addAll(Arrays.asList("hot", "warm", "comfort-food", "soup"));
        }

        // Time-based recommendations
        if (isBreakfastTime()) {
            tags.addAll(Arrays.asList("breakfast", "light", "healthy"));
        } else if (isLunchTime()) {
            tags.addAll(Arrays.asList("lunch", "filling", "energy"));
        } else if (isDinnerTime()) {
            tags.addAll(Arrays.asList("dinner", "hearty", "satisfying"));
        } else if (isLateNight()) {
            tags.addAll(Arrays.asList("light", "quick", "comfort"));
        }

        // Season-based recommendations
        if (isSummerSeason()) {
            tags.addAll(Arrays.asList("fresh", "light", "cooling"));
        } else if (isWinterSeason()) {
            tags.addAll(Arrays.asList("warming", "hearty", "comfort"));
        }

        // Weather-specific recommendations
        if (isRainyWeather()) {
            tags.addAll(Arrays.asList("comfort-food", "warm", "indoor"));
        } else if (isSunnyWeather()) {
            tags.addAll(Arrays.asList("fresh", "outdoor", "light"));
        }

        // Group size recommendations
        if (isGroupOrder()) {
            tags.addAll(Arrays.asList("sharing", "variety", "popular"));
        }

        return tags;
    }

    public double getContextualMultiplier(MenuItem item) {
        if (item == null) return 1.0;

        double multiplier = 1.0;
        List<String> itemTags = item.getTags();
        List<String> recommendedTags = getRecommendedTags();

        // Boost items that match contextual recommendations
        for (String tag : recommendedTags) {
            if (itemTags.contains(tag)) {
                multiplier += 0.1;
            }
        }

        // Special contextual adjustments
        if (isHotWeather() && item.hasTag("hot")) {
            multiplier -= 0.2; // Reduce hot foods in hot weather
        } else if (isColdWeather() && item.hasTag("cold")) {
            multiplier -= 0.2; // Reduce cold foods in cold weather
        }

        // Time-specific adjustments
        if (isLateNight() && item.hasTag("heavy")) {
            multiplier -= 0.3; // Reduce heavy foods late at night
        }

        return Math.max(0.1, Math.min(2.0, multiplier));
    }

    // Utility methods
    public String getContextDescription() {
        return String.format("%s %s in %s (%.1f°C)", 
                           timeOfDay, season, weather != null ? weather : "unknown weather", temperature);
    }

    public boolean isAppropriateForContext(MenuItem item) {
        double multiplier = getContextualMultiplier(item);
        return multiplier >= 1.0; // Items with multiplier >= 1.0 are appropriate
    }

    // Getters and Setters
    public String getSeason() { return season; }
    public void setSeason(String season) { this.season = season; }

    public String getTimeOfDay() { return timeOfDay; }
    public void setTimeOfDay(String timeOfDay) { this.timeOfDay = timeOfDay; }

    public String getWeather() { return weather; }
    public void setWeather(String weather) { this.weather = weather; }

    public LocalDateTime getCurrentTime() { return currentTime; }
    public void setCurrentTime(LocalDateTime currentTime) { 
        this.currentTime = currentTime;
        if (currentTime != null) {
            determineTimeOfDay();
            determineSeason();
            determineWeekend();
        }
    }

    public boolean isWeekend() { return isWeekend; }
    public void setWeekend(boolean weekend) { isWeekend = weekend; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public int getGroupSize() { return groupSize; }
    public void setGroupSize(int groupSize) { this.groupSize = Math.max(1, groupSize); }

    @Override
    public String toString() {
        return String.format("ContextualFactor{season='%s', timeOfDay='%s', weather='%s', temp=%.1f°C, weekend=%b}", 
                           season, timeOfDay, weather, temperature, isWeekend);
    }
}
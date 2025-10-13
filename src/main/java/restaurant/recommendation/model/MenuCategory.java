package restaurant.recommendation.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * MenuCategory class with reflexive association for hierarchical categories
 * As per UML: MenuCategory 0..1 -- 0..* MenuCategory (parentCategory-childCategories)
 */
public class MenuCategory {
    // UML specified attributes
    private String categoryId;
    private String categoryName;
    private String description;

    // Reflexive association attributes (0..1 parent, 0..* children)
    private MenuCategory parentCategory;
    private List<MenuCategory> childCategories;

    // Associated menu items
    private List<MenuItem> menuItems;

    // Constructors
    public MenuCategory() {
        this.childCategories = new ArrayList<>();
        this.menuItems = new ArrayList<>();
    }

    public MenuCategory(String categoryId, String categoryName) {
        this();
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    public MenuCategory(String categoryId, String categoryName, String description) {
        this(categoryId, categoryName);
        this.description = description;
    }

    // Menu item management (association relationship)
    public void addMenuItem(MenuItem menuItem) {
        if (menuItem != null && !menuItems.contains(menuItem)) {
            menuItems.add(menuItem);
            menuItem.setCategory(this); // Maintain bidirectional relationship
        }
    }

    public void removeMenuItem(MenuItem menuItem) {
        if (menuItems.remove(menuItem) && menuItem != null) {
            // Only clear category if it was this category
            if (this.equals(menuItem.getCategory())) {
                menuItem.setCategory(null);
            }
        }
    }

    // Hierarchical category management (reflexive association)
    public void setParentCategory(MenuCategory parentCategory) {
        // Remove from old parent if exists
        if (this.parentCategory != null) {
            this.parentCategory.removeChildCategory(this);
        }

        this.parentCategory = parentCategory;

        // Add to new parent if not null
        if (parentCategory != null) {
            parentCategory.addChildCategory(this);
        }
    }

    public void addChildCategory(MenuCategory childCategory) {
        if (childCategory != null && !childCategories.contains(childCategory)) {
            childCategories.add(childCategory);

            // Ensure bidirectional relationship
            if (!this.equals(childCategory.getParentCategory())) {
                childCategory.parentCategory = this; // Direct assignment to avoid recursion
            }
        }
    }

    public void removeChildCategory(MenuCategory childCategory) {
        if (childCategories.remove(childCategory) && childCategory != null) {
            // Clear parent reference if it was this category
            if (this.equals(childCategory.getParentCategory())) {
                childCategory.parentCategory = null;
            }
        }
    }

    // Utility methods for hierarchy navigation
    public boolean isRootCategory() {
        return parentCategory == null;
    }

    public boolean isLeafCategory() {
        return childCategories.isEmpty();
    }

    public int getDepthLevel() {
        int depth = 0;
        MenuCategory current = this.parentCategory;
        while (current != null) {
            depth++;
            current = current.parentCategory;
        }
        return depth;
    }

    public MenuCategory getRootCategory() {
        MenuCategory current = this;
        while (current.parentCategory != null) {
            current = current.parentCategory;
        }
        return current;
    }

    public List<MenuCategory> getPath() {
        List<MenuCategory> path = new ArrayList<>();
        MenuCategory current = this;
        while (current != null) {
            path.add(0, current); // Add at beginning for root-to-leaf order
            current = current.parentCategory;
        }
        return path;
    }

    public String getFullPath() {
        List<MenuCategory> path = getPath();
        StringBuilder fullPath = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) {
                fullPath.append(" > ");
            }
            fullPath.append(path.get(i).getCategoryName());
        }
        return fullPath.toString();
    }

    public List<MenuCategory> getAllDescendants() {
        List<MenuCategory> descendants = new ArrayList<>();
        for (MenuCategory child : childCategories) {
            descendants.add(child);
            descendants.addAll(child.getAllDescendants()); // Recursive
        }
        return descendants;
    }

    public List<MenuItem> getAllMenuItems() {
        List<MenuItem> allItems = new ArrayList<>(menuItems);

        // Add items from all descendant categories
        for (MenuCategory child : childCategories) {
            allItems.addAll(child.getAllMenuItems());
        }

        return allItems;
    }

    public int getTotalItemCount() {
        int count = menuItems.size();
        for (MenuCategory child : childCategories) {
            count += child.getTotalItemCount();
        }
        return count;
    }

    public MenuCategory findCategoryById(String categoryId) {
        if (Objects.equals(this.categoryId, categoryId)) {
            return this;
        }

        // Search in child categories
        for (MenuCategory child : childCategories) {
            MenuCategory found = child.findCategoryById(categoryId);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    public List<MenuCategory> findCategoriesByName(String name) {
        List<MenuCategory> matches = new ArrayList<>();

        if (categoryName != null && categoryName.toLowerCase().contains(name.toLowerCase())) {
            matches.add(this);
        }

        // Search in child categories
        for (MenuCategory child : childCategories) {
            matches.addAll(child.findCategoriesByName(name));
        }

        return matches;
    }

    // Validation methods
    public boolean isValidHierarchy() {
        // Check for circular references
        MenuCategory current = this.parentCategory;
        while (current != null) {
            if (current == this) {
                return false; // Circular reference detected
            }
            current = current.parentCategory;
        }
        return true;
    }

    // Getters and Setters
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public MenuCategory getParentCategory() { return parentCategory; }

    public List<MenuCategory> getChildCategories() { 
        return new ArrayList<>(childCategories); 
    }

    public List<MenuItem> getMenuItems() { 
        return new ArrayList<>(menuItems); 
    }

    public void setMenuItems(List<MenuItem> menuItems) { 
        this.menuItems = menuItems != null ? new ArrayList<>(menuItems) : new ArrayList<>();
    }

    // Object methods
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MenuCategory that = (MenuCategory) o;
        return Objects.equals(categoryId, that.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(categoryId);
    }

    @Override
    public String toString() {
        return String.format("MenuCategory{id='%s', name='%s', items=%d, children=%d}", 
                           categoryId, categoryName, menuItems.size(), childCategories.size());
    }
}
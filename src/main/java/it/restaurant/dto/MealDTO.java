package it.restaurant.dto;

public class MealDTO {

    /**** Fields ****/
    private String mealCode;
    private String category;
    private String name;
    private double price;
    private String description;
    private int availability;

    /**** Constructors ****/
    // Empty Constructor for Jackson
    public MealDTO() {
    }

    // Complete Constructor
    public MealDTO(String mealCode, String category, String name, double price, String description, int availability) {
        this.mealCode = mealCode;
        this.category = category;
        this.name = name;
        this.price = price;
        this.description = description;
        this.availability = availability;
    }

    /**** Getter and Setter Methods ****/
    public String getMealCode() {
        return mealCode;
    }

    public void setMealCode(String mealCode) {
        this.mealCode = mealCode;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getAvailability() {
        return availability;
    }

    public void setAvailability(int availability) {
        this.availability = availability;
    }
}
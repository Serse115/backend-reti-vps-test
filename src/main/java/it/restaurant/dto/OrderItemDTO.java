package it.restaurant.dto;

/******** DTO for Order Item ********/
public class OrderItemDTO {

    /**** Fields ****/
    private String mealCode;
    private String mealName;
    private int quantity;
    private String specialRequests;

    /**** Constructors ****/
    // Empty constructor for the Jackson
    public OrderItemDTO() {
    }

    // Full constructor for the DB
    public OrderItemDTO(String mealCode, String mealName, int quantity, String specialRequests) {
        this.mealCode = mealCode;
        this.mealName = mealName;
        this.quantity = quantity;
        this.specialRequests = specialRequests;
    }

    /**** Getters and Setters ****/
    public String getMealCode() {
        return mealCode;
    }

    public void setMealCode(String mealCode) {
        this.mealCode = mealCode;
    }

    public String getMealName() {
        return mealName;
    }

    public void setMealName(String mealName) {
        this.mealName = mealName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getSpecialRequests() {
        return specialRequests;
    }

    public void setSpecialRequests(String specialRequests) {
        this.specialRequests = specialRequests;
    }
}
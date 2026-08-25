package it.restaurant.dto;

/******** DTO for Delivery Orders Summary (Quick order overview) ********/
public class OrderSummaryDTO {

    /**** Fields ****/
    private String orderCode;
    private String orderStatus;

    /**** Constructors ****/
    // Empty constuctor for the Jackson
    public OrderSummaryDTO() {
    }

    // Full contructor for the ResultSet/Database
    public OrderSummaryDTO(String orderCode, String orderStatus) {
        this.orderCode = orderCode;
        this.orderStatus = orderStatus;
    }

    /**** Getters and Setters ****/
    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }
}
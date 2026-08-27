package it.restaurant.dto;

/******** DTO for Order Status Update Request ********/
public class UpdateStatusRequestDTO {

    /**** Fields ****/
    private String orderCode;
    private String status;

    /**** Constructors ****/
    // Empty constructor for Jackson
    public UpdateStatusRequestDTO() {
    }

    // Full constructor for the Database
    public UpdateStatusRequestDTO(String orderCode, String status) {
        this.orderCode = orderCode;
        this.status = status;
    }

    /**** Getters and Setters ****/
    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
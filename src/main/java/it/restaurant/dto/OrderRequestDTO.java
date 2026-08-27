package it.restaurant.dto;

import java.util.List;

/******** DTO for Order request item ********/
// DATA TRANSFER OBJECT for the order itself
public class OrderRequestDTO {

    /**** Fields ****/
    private String orderCode;
    private String name;
    private String address;
    private String cap;
    private String city;
    private String email;
    private String phone;
    public List<OrderItemDTO> items;       // List of items (chosen meals)

    /**** Constructors ****/
    public OrderRequestDTO() {

    }

    /**** Getters and Setters methods ****/
    public String getOrderCode() {
        return this.orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return this.address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCap() {
        return this.cap;
    }

    public void setCap(String price) {
        this.cap = cap;
    }

    public String getCity() {
        return this.city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return this.phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public List<OrderItemDTO> getItems() {
        return this.items;
    }
}
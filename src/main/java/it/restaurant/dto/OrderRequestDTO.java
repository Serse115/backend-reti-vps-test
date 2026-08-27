package it.restaurant.dto;

import java.util.List;

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
    private List<OrderItemDTO> items; // La nostra lista di piatti

    public OrderRequestDTO() {

    }

    // Getters

    public String getOrderCode() {
        return orderCode;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getCap() {
        return cap;
    }

    public String getCity() {
        return city;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public List<OrderItemDTO> getItems() {
        return items;
    }

    // Setters

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setCap(String cap) {
        this.cap = cap;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setItems(List<OrderItemDTO> items) {
        this.items = items;
    }
}
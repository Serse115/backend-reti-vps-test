package it.restaurant.dto;

import java.util.ArrayList;
import java.util.List;

public class DeliveryOrdersFullListDTO {

    /**** Fields ****/
    private String orderCode;
    private String orderStatus;
    private List<OrderItemDTO> items;

    /**** Constructors ****/
    // Empty Constructor for Jackson
    public DeliveryOrdersFullListDTO() {
        this.items = new ArrayList<>();
    }

    // Complete Constructor
    public DeliveryOrdersFullListDTO(String orderCode, String orderStatus, List<OrderItemDTO> items) {
        this.orderCode = orderCode;
        this.orderStatus = orderStatus;
        this.items = items != null ? items : new ArrayList<>();
    }

    /**** Getter and Setter Methods ****/
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

    public List<OrderItemDTO> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDTO> items) {
        this.items = items;
    }
}
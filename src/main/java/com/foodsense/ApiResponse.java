package com.foodsense;

public class ApiResponse {
    private int status;
    private Product product;
    private String code;

    public int getStatus(){
        return status;
    }

    public Product getProduct() {
        return product;
    }

    public String getCode() {
        return code;
    }
}

package com.scc.model;

public record Order(int orderId,
                    int userId,
                    double totalAmount,
                    String paymentMethod,
                    String deliveryAddress,
                    String orderStatus) {
}
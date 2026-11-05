package com.scc.model;

public record Product(int productId, String productName, String category, String description,
                      String size, String color, double price, int stockQuantity,
                      int reorderLevel, String status, String dateAdded) {}
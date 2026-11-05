package com.scc.dao;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class OrderDao {
    private final ActivityLogDao logDao = new ActivityLogDao();

    // Core Business Process: Sale. cart = productId -> quantity
    // Transaction: validate stock, insert order + items, deduct stock (all or nothing)
    public int placeOrder(int customerId, String customerName, Map<Integer, Integer> cart,
                          String paymentMethod, String deliveryAddress) {
        if (cart == null || cart.isEmpty()) return -1;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            int orderId;
            double total = 0;
            try {
                Map<Integer, Double> prices = new HashMap<>();

                for (Map.Entry<Integer, Integer> e : cart.entrySet()) {
                    String check = "SELECT price, stock_quantity, status FROM products WHERE product_id = ?";
                    try (PreparedStatement p = conn.prepareStatement(check)) {
                        p.setInt(1, e.getKey());
                        try (ResultSet rs = p.executeQuery()) {
                            if (!rs.next() || !"ACTIVE".equals(rs.getString("status"))) {
                                System.out.println("-> Product #" + e.getKey() + " is not available.");
                                conn.rollback();
                                return -1;
                            }
                            if (rs.getInt("stock_quantity") < e.getValue()) {
                                System.out.println("-> Not enough stock for Product #" + e.getKey() + ".");
                                conn.rollback();
                                return -1;
                            }
                            double price = rs.getDouble("price");
                            prices.put(e.getKey(), price);
                            total += price * e.getValue();
                        }
                    }
                }

                String insertOrder = "INSERT INTO orders(user_id, total_amount, payment_method, delivery_address) VALUES(?, ?, ?, ?)";
                try (PreparedStatement p = conn.prepareStatement(insertOrder, Statement.RETURN_GENERATED_KEYS)) {
                    p.setInt(1, customerId);
                    p.setDouble(2, total);
                    p.setString(3, paymentMethod);
                    p.setString(4, deliveryAddress);
                    p.executeUpdate();
                    try (ResultSet keys = p.getGeneratedKeys()) {
                        if (!keys.next()) {
                            conn.rollback();
                            return -1;
                        }
                        orderId = keys.getInt(1);
                    }
                }

                String insertItem = "INSERT INTO order_items(order_id, product_id, quantity, unit_price, subtotal) VALUES(?, ?, ?, ?, ?)";
                String deduct = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE product_id = ?";
                for (Map.Entry<Integer, Integer> e : cart.entrySet()) {
                    double price = prices.get(e.getKey());
                    try (PreparedStatement p = conn.prepareStatement(insertItem)) {
                        p.setInt(1, orderId);
                        p.setInt(2, e.getKey());
                        p.setInt(3, e.getValue());
                        p.setDouble(4, price);
                        p.setDouble(5, price * e.getValue());
                        p.executeUpdate();
                    }
                    try (PreparedStatement p = conn.prepareStatement(deduct)) {
                        p.setInt(1, e.getValue());
                        p.setInt(2, e.getKey());
                        p.executeUpdate();
                    }
                }

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }

            logDao.logAction(customerId, "ORDER_CREATED",
                    "Placed Order #" + orderId + " for Customer " + customerName
                            + " - Total: P" + String.format("%.2f", total));
            return orderId;
        } catch (SQLException e) {
            System.err.println("Error placing order: " + e.getMessage());
            return -1;
        }
    }

    private String getOrderStatus(Connection conn, int orderId) throws SQLException {
        try (PreparedStatement p = conn.prepareStatement("SELECT order_status FROM orders WHERE order_id = ?")) {
            p.setInt(1, orderId);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next() ? rs.getString("order_status") : null;
            }
        }
    }

    // Lifecycle / State Change
    public boolean updateOrderStatus(int staffId, int orderId, String newStatus) {
        String sql = "UPDATE orders SET order_status = ?, processed_by = ? WHERE order_id = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            String oldStatus = getOrderStatus(conn, orderId);
            if (oldStatus == null) return false;

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, newStatus);
                pstmt.setInt(2, staffId);
                pstmt.setInt(3, orderId);
                boolean ok = pstmt.executeUpdate() > 0;
                if (ok) logDao.logAction(staffId, "ORDER_UPDATED",
                        "Updated Order #" + orderId + " status from " + oldStatus + " to " + newStatus);
                return ok;
            }
        } catch (SQLException e) {
            System.err.println("Error updating order status: " + e.getMessage());
            return false;
        }
    }
}
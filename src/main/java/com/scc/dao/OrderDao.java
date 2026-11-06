package com.scc.dao;

import com.scc.model.Order;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderDao {
    private final ActivityLogDao logDao = new ActivityLogDao();

    public record Item(int productId, int quantity, double unitPrice, double subtotal) {
    }


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


    private static final String SELECT_ORDERS =
            "SELECT order_id, user_id, total_amount, payment_method, delivery_address, order_status FROM orders ";

    private List<Order> fetchOrders(String sql, Object param) {
        List<Order> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement p = conn.prepareStatement(sql)) {
            if (param != null) p.setObject(1, param);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    list.add(new Order(
                            rs.getInt("order_id"),
                            rs.getInt("user_id"),
                            rs.getDouble("total_amount"),
                            rs.getString("payment_method"),
                            rs.getString("delivery_address"),
                            rs.getString("order_status")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading orders: " + e.getMessage());
        }
        return list;
    }

    public List<Order> getAllOrders() {
        return fetchOrders(SELECT_ORDERS + "ORDER BY order_id DESC", null);
    }

    public Order getOrderById(int orderId) {
        List<Order> list = fetchOrders(SELECT_ORDERS + "WHERE order_id = ?", orderId);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Order> getOrdersByUserId(int userId) {
        return fetchOrders(SELECT_ORDERS + "WHERE user_id = ? ORDER BY order_id DESC", userId);
    }

    public List<Order> getOrdersByStatus(String status) {
        return fetchOrders(SELECT_ORDERS + "WHERE UPPER(order_status) = ? ORDER BY order_id DESC",
                status.toUpperCase());
    }

    public List<Item> getOrderItems(int orderId) {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT product_id, quantity, unit_price, subtotal FROM order_items WHERE order_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setInt(1, orderId);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    items.add(new Item(
                            rs.getInt("product_id"),
                            rs.getInt("quantity"),
                            rs.getDouble("unit_price"),
                            rs.getDouble("subtotal")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading order items: " + e.getMessage());
        }
        return items;
    }


    private String getOrderStatus(Connection conn, int orderId) throws SQLException {
        try (PreparedStatement p = conn.prepareStatement("SELECT order_status FROM orders WHERE order_id = ?")) {
            p.setInt(1, orderId);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next() ? rs.getString("order_status") : null;
            }
        }
    }

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


    public boolean cancelOrder(int staffId, int orderId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            String oldStatus;
            try {
                oldStatus = getOrderStatus(conn, orderId);
                if (oldStatus == null
                        || !(oldStatus.equalsIgnoreCase("PENDING") || oldStatus.equalsIgnoreCase("PROCESSING"))) {
                    conn.rollback();
                    return false;
                }

                try (PreparedStatement p = conn.prepareStatement(
                        "UPDATE orders SET order_status = 'CANCELLED', processed_by = ? WHERE order_id = ?")) {
                    p.setInt(1, staffId);
                    p.setInt(2, orderId);
                    p.executeUpdate();
                }

                List<int[]> toRestore = new ArrayList<>();
                try (PreparedStatement p = conn.prepareStatement(
                        "SELECT product_id, quantity FROM order_items WHERE order_id = ?")) {
                    p.setInt(1, orderId);
                    try (ResultSet rs = p.executeQuery()) {
                        while (rs.next()) {
                            toRestore.add(new int[]{rs.getInt("product_id"), rs.getInt("quantity")});
                        }
                    }
                }
                for (int[] row : toRestore) {
                    try (PreparedStatement p = conn.prepareStatement(
                            "UPDATE products SET stock_quantity = stock_quantity + ? WHERE product_id = ?")) {
                        p.setInt(1, row[1]);
                        p.setInt(2, row[0]);
                        p.executeUpdate();
                    }
                }

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }

            logDao.logAction(staffId, "ORDER_CANCELLED",
                    "Cancelled Order #" + orderId + " (was " + oldStatus + "). Stock restored.");
            return true;
        } catch (SQLException e) {
            System.err.println("Error cancelling order: " + e.getMessage());
            return false;
        }
    }


    public boolean cancelOwnOrder(int customerId, int orderId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement p = conn.prepareStatement(
                        "SELECT order_status FROM orders WHERE order_id = ? AND user_id = ?")) {
                    p.setInt(1, orderId);
                    p.setInt(2, customerId);
                    try (ResultSet rs = p.executeQuery()) {
                        if (!rs.next() || !"PENDING".equalsIgnoreCase(rs.getString("order_status"))) {
                            conn.rollback();
                            return false;
                        }
                    }
                }

                try (PreparedStatement p = conn.prepareStatement(
                        "UPDATE orders SET order_status = 'CANCELLED' WHERE order_id = ? AND user_id = ?")) {
                    p.setInt(1, orderId);
                    p.setInt(2, customerId);
                    p.executeUpdate();
                }

                List<int[]> toRestore = new ArrayList<>();
                try (PreparedStatement p = conn.prepareStatement(
                        "SELECT product_id, quantity FROM order_items WHERE order_id = ?")) {
                    p.setInt(1, orderId);
                    try (ResultSet rs = p.executeQuery()) {
                        while (rs.next()) {
                            toRestore.add(new int[]{rs.getInt("product_id"), rs.getInt("quantity")});
                        }
                    }
                }
                for (int[] row : toRestore) {
                    try (PreparedStatement p = conn.prepareStatement(
                            "UPDATE products SET stock_quantity = stock_quantity + ? WHERE product_id = ?")) {
                        p.setInt(1, row[1]);
                        p.setInt(2, row[0]);
                        p.executeUpdate();
                    }
                }

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }

            logDao.logAction(customerId, "ORDER_CANCELLED",
                    "Customer cancelled own Order #" + orderId + ". Stock restored.");
            return true;
        } catch (SQLException e) {
            System.err.println("Error cancelling order: " + e.getMessage());
            return false;
        }
    }
}
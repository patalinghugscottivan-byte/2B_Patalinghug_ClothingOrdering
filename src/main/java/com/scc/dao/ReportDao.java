package com.scc.dao;

import com.scc.model.Order;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReportDao {

    public record Summary(int totalOrders, int cancelledOrders, double netSales) {
    }

    public record DayRow(String day, int orders, double sales) {
    }

    private static final String[] DATE_CANDIDATES = {
            "order_date", "created_at", "date_ordered", "order_datetime",
            "ordered_at", "created_on", "date", "timestamp"
    };

    private String cachedDateColumn;
    private boolean checked = false;

    /** Automatically finds the date column in the orders table. */
    public String dateColumn() {
        if (checked) return cachedDateColumn;
        checked = true;

        List<String> cols = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(orders)")) {
            while (rs.next()) cols.add(rs.getString("name"));
        } catch (SQLException e) {
            System.err.println("Error reading orders table: " + e.getMessage());
            return null;
        }

        for (String candidate : DATE_CANDIDATES) {
            for (String col : cols) {
                if (col.equalsIgnoreCase(candidate)) {
                    cachedDateColumn = col;
                    return col;
                }
            }
        }
        for (String col : cols) {
            String l = col.toLowerCase();
            if (l.contains("date") || l.contains("created") || l.contains("time")) {
                cachedDateColumn = col;
                return col;
            }
        }
        return null;
    }

    private boolean safe(String col) {
        return col != null && col.matches("[A-Za-z0-9_]+");
    }

    /** period = "YYYY-MM-DD" (daily) or "YYYY-MM" (monthly) */
    public Summary getSummary(String period) {
        String col = dateColumn();
        if (!safe(col)) return new Summary(0, 0, 0);

        String sql = "SELECT COUNT(*) AS total, "
                + "COALESCE(SUM(CASE WHEN UPPER(order_status) = 'CANCELLED' THEN 1 ELSE 0 END), 0) AS cancelled, "
                + "COALESCE(SUM(CASE WHEN UPPER(order_status) <> 'CANCELLED' THEN total_amount ELSE 0 END), 0) AS sales "
                + "FROM orders WHERE substr(" + col + ", 1, " + period.length() + ") = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setString(1, period);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) {
                    return new Summary(rs.getInt("total"), rs.getInt("cancelled"), rs.getDouble("sales"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading sales summary: " + e.getMessage());
        }
        return new Summary(0, 0, 0);
    }

    /** Per-day breakdown within a month (YYYY-MM) */
    public List<DayRow> getDailyBreakdown(String yearMonth) {
        List<DayRow> rows = new ArrayList<>();
        String col = dateColumn();
        if (!safe(col)) return rows;

        String sql = "SELECT substr(" + col + ", 1, 10) AS day, COUNT(*) AS orders, "
                + "COALESCE(SUM(CASE WHEN UPPER(order_status) <> 'CANCELLED' THEN total_amount ELSE 0 END), 0) AS sales "
                + "FROM orders WHERE substr(" + col + ", 1, 7) = ? GROUP BY day ORDER BY day";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setString(1, yearMonth);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    rows.add(new DayRow(rs.getString("day"), rs.getInt("orders"), rs.getDouble("sales")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading monthly breakdown: " + e.getMessage());
        }
        return rows;
    }

    /** All orders on a given day (YYYY-MM-DD) */
    public List<Order> getOrdersOn(String day) {
        List<Order> list = new ArrayList<>();
        String col = dateColumn();
        if (!safe(col)) return list;

        String sql = "SELECT order_id, user_id, total_amount, payment_method, delivery_address, order_status "
                + "FROM orders WHERE substr(" + col + ", 1, 10) = ? ORDER BY order_id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setString(1, day);
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
}
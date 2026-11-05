package com.scc.dao;

import com.scc.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {
    private final ActivityLogDao logDao = new ActivityLogDao();

    public ProductDao() {
        seedSampleProducts();
    }

    private void seedSampleProducts() {
        String countSql = "SELECT COUNT(*) FROM products";
        String insertSql = "INSERT INTO products(product_name, category, description, size, color, price, stock_quantity, reorder_level) VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
        Object[][] samples = {
                {"Basic White T-Shirt", "Tops", "Cotton crew neck tee", "M", "White", 299.00, 50, 10},
                {"Black Polo Shirt", "Tops", "Classic fit polo", "L", "Black", 449.00, 30, 8},
                {"Slim Fit Jeans", "Bottoms", "Stretch denim, slim fit", "32", "Blue", 899.00, 25, 5},
                {"Cargo Shorts", "Bottoms", "Six-pocket cargo shorts", "M", "Khaki", 549.00, 20, 5},
                {"Pullover Hoodie", "Outerwear", "Fleece-lined hoodie", "L", "Gray", 1199.00, 15, 5},
                {"Floral Summer Dress", "Dresses", "Lightweight knee-length dress", "S", "Pink", 799.00, 12, 4}
        };
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                try (PreparedStatement p = conn.prepareStatement(insertSql)) {
                    for (Object[] s : samples) {
                        p.setString(1, (String) s[0]);
                        p.setString(2, (String) s[1]);
                        p.setString(3, (String) s[2]);
                        p.setString(4, (String) s[3]);
                        p.setString(5, (String) s[4]);
                        p.setDouble(6, (Double) s[5]);
                        p.setInt(7, (Integer) s[6]);
                        p.setInt(8, (Integer) s[7]);
                        p.executeUpdate();
                    }
                    System.out.println("-> Sample products added.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error seeding products: " + e.getMessage());
        }
    }

    private Product mapProduct(ResultSet rs) throws SQLException {
        return new Product(
                rs.getInt("product_id"),
                rs.getString("product_name"),
                rs.getString("category"),
                rs.getString("description"),
                rs.getString("size"),
                rs.getString("color"),
                rs.getDouble("price"),
                rs.getInt("stock_quantity"),
                rs.getInt("reorder_level"),
                rs.getString("status"),
                rs.getString("date_added"));
    }

    // CREATE
    public int addProduct(int userId, String name, String category, String description,
                          String size, String color, double price, int stock, int reorderLevel) {
        String sql = "INSERT INTO products(product_name, category, description, size, color, price, stock_quantity, reorder_level) VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement p = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, name);
            p.setString(2, category);
            p.setString(3, description);
            p.setString(4, size);
            p.setString(5, color);
            p.setDouble(6, price);
            p.setInt(7, stock);
            p.setInt(8, reorderLevel);
            p.executeUpdate();
            try (ResultSet keys = p.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    logDao.logAction(userId, "PRODUCT_ADDED",
                            "User " + userId + " added Product #" + id + " " + name);
                    return id;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error adding product: " + e.getMessage());
        }
        return -1;
    }

    // READ
    public List<Product> getAllProducts() {
        return query("SELECT * FROM products ORDER BY product_id", new ArrayList<>());
    }

    public List<Product> searchProducts(String keyword, String category, Double minPrice, Double maxPrice) {
        StringBuilder sql = new StringBuilder("SELECT * FROM products WHERE status = 'ACTIVE'");
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND LOWER(product_name) LIKE ?");
            params.add("%" + keyword.toLowerCase() + "%");
        }
        if (category != null && !category.isEmpty()) {
            sql.append(" AND LOWER(category) = ?");
            params.add(category.toLowerCase());
        }
        if (minPrice != null) {
            sql.append(" AND price >= ?");
            params.add(minPrice);
        }
        if (maxPrice != null) {
            sql.append(" AND price <= ?");
            params.add(maxPrice);
        }
        sql.append(" ORDER BY product_id");
        return query(sql.toString(), params);
    }

    public List<Product> getLowStock() {
        return query("SELECT * FROM products WHERE status = 'ACTIVE' AND stock_quantity <= reorder_level ORDER BY stock_quantity",
                new ArrayList<>());
    }

    private List<Product> query(String sql, List<Object> params) {
        List<Product> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement p = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) p.setObject(i + 1, params.get(i));
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) list.add(mapProduct(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching products: " + e.getMessage());
        }
        return list;
    }

    public List<String> getCategories() {
        List<String> cats = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM products WHERE status = 'ACTIVE' ORDER BY category";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement p = conn.prepareStatement(sql);
             ResultSet rs = p.executeQuery()) {
            while (rs.next()) cats.add(rs.getString(1));
        } catch (SQLException e) {
            System.err.println("Error fetching categories: " + e.getMessage());
        }
        return cats;
    }

    public Product getProductById(int productId) {
        String sql = "SELECT * FROM products WHERE product_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setInt(1, productId);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) return mapProduct(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching product: " + e.getMessage());
        }
        return null;
    }

    // UPDATE
    public boolean updatePrice(int userId, int productId, double newPrice) {
        Product old = getProductById(productId);
        if (old == null) return false;
        String sql = "UPDATE products SET price = ? WHERE product_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setDouble(1, newPrice);
            p.setInt(2, productId);
            boolean ok = p.executeUpdate() > 0;
            if (ok) logDao.logAction(userId, "PRICE_UPDATED",
                    "Product #" + productId + " price from P" + String.format("%.2f", old.price())
                            + " to P" + String.format("%.2f", newPrice));
            return ok;
        } catch (SQLException e) {
            System.err.println("Error updating price: " + e.getMessage());
            return false;
        }
    }

    public boolean updateStock(int userId, int productId, int newStock) {
        Product old = getProductById(productId);
        if (old == null) return false;
        String sql = "UPDATE products SET stock_quantity = ? WHERE product_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setInt(1, newStock);
            p.setInt(2, productId);
            boolean ok = p.executeUpdate() > 0;
            if (ok) logDao.logAction(userId, "STOCK_UPDATED",
                    "Product #" + productId + " stock from " + old.stockQuantity() + " to " + newStock);
            return ok;
        } catch (SQLException e) {
            System.err.println("Error updating stock: " + e.getMessage());
            return false;
        }
    }

    public boolean discontinueProduct(int userId, int productId) {
        String sql = "UPDATE products SET status = 'DISCONTINUED' WHERE product_id = ? AND status = 'ACTIVE'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setInt(1, productId);
            boolean ok = p.executeUpdate() > 0;
            if (ok) logDao.logAction(userId, "PRODUCT_DISABLED",
                    "User " + userId + " discontinued Product #" + productId);
            return ok;
        } catch (SQLException e) {
            System.err.println("Error discontinuing product: " + e.getMessage());
            return false;
        }
    }
}
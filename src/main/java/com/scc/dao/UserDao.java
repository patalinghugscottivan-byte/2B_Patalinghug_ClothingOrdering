package com.scc.dao;

import com.scc.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public UserDao() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            defaultAdminGenerator(conn);
        } catch (SQLException e) {
            System.err.println("Error seeding default admin: " + e.getMessage());
        }
    }

    public void defaultAdminGenerator(Connection conn) {
        String checkSql = "SELECT COUNT(*) FROM users WHERE role ='ADMIN'";
        String insertSql = "INSERT INTO users(fullname, username, password, email, contact_number, address, role, status) VALUES('admin', 'admin', 'admin1', 'admin@gmail.com', '09111111111', 'admin', 'ADMIN', 'APPROVED')";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.executeUpdate(insertSql);
                System.out.println("-> Default Admin created");
            }
        } catch (SQLException e) {
            System.err.println("Error seeding default admin: " + e.getMessage());
        }
    }

    public boolean registerUser(String fullname, String username, String password, String email, String contact_number, String address, String role) {

        String sql = "INSERT INTO users(fullname, username, password, email, contact_number, address, role, status) VALUES(?, ?, ?, ?, ?, ?, ?, 'PENDING')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, fullname);
            pstmt.setString(2, username);
            pstmt.setString(3, password);
            pstmt.setString(4, email);
            pstmt.setString(5, contact_number);
            pstmt.setString(6, address);
            pstmt.setString(7, role);
            pstmt.executeUpdate();
            System.out.println("-> Successful Registration, Waiting for Approval!");
            return true;
        } catch (SQLException e) {
            System.err.println("Failed " + e.getMessage());
            return false;
        }
    }

    public User login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString("status");
                    if ("PENDING".equalsIgnoreCase(status)) {
                        System.out.println("-> Account is pending, Waiting for Approval");
                        return null;
                    }
                    if ("INACTIVE".equalsIgnoreCase(status)) {
                        System.out.println("-> Account is deactivated. Contact an admin.");
                        return null;
                    }
                    return mapUser(rs);
                } else {
                    System.out.println(" -> Invalid username or password");
                }
            }

        } catch (SQLException e) {
            System.err.println("Error during login " + e.getMessage());
        }
        return null;
    }

    // ---------- MANAGE USERS ----------

    private User mapUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getString("fullname"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("email"),
                rs.getString("contact_number"),
                rs.getString("address"),
                rs.getString("role"),
                rs.getString("status"),
                rs.getString("date_created")
        );
    }

    // statusFilter = null para tanan
    public List<User> getUsers(String statusFilter) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users"
                + (statusFilter != null ? " WHERE status = ?" : "")
                + " ORDER BY user_id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (statusFilter != null) pstmt.setString(1, statusFilter);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) users.add(mapUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching users: " + e.getMessage());
        }
        return users;
    }

    public User getUserById(int userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user: " + e.getMessage());
        }
        return null;
    }

    public boolean updateStatus(int userId, String status) {
        String sql = "UPDATE users SET status = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating status: " + e.getMessage());
            return false;
        }
    }

    public boolean updateRole(int userId, String role) {
        String sql = "UPDATE users SET role = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, role);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating role: " + e.getMessage());
            return false;
        }
    }
}
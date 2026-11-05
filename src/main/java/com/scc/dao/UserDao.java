package com.scc.dao;

import com.scc.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {
    private final ActivityLogDao logDao = new ActivityLogDao();

    public UserDao() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            defaultAdminGenerator(conn);
        } catch (SQLException e) {
            System.err.println("Error seeding default admin: " + e.getMessage());
        }
    }

    public void defaultAdminGenerator(Connection conn) {
        String checkSql = "SELECT COUNT(*) FROM users WHERE role = ?";
        String insertSql = "INSERT INTO users(fullname, username, password, email, contact_number, address, role, status) VALUES(?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement check = conn.prepareStatement(checkSql)) {
            check.setString(1, "ADMIN");
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                        ins.setString(1, "admin");
                        ins.setString(2, "admin");
                        ins.setString(3, "admin1");
                        ins.setString(4, "admin@gmail.com");
                        ins.setString(5, "09111111111");
                        ins.setString(6, "admin");
                        ins.setString(7, "ADMIN");
                        ins.setString(8, "APPROVED");
                        ins.executeUpdate();
                        System.out.println("-> Default Admin created");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error seeding default admin: " + e.getMessage());
        }
    }

    public boolean registerUser(String fullname, String username, String password, String email,
                                String contact_number, String address, String role) {

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

    // Admin creates an account directly (status APPROVED, no waiting)
    public int createUser(int adminId, String fullname, String username, String password,
                          String email, String contactNumber, String address, String role) {
        String sql = "INSERT INTO users(fullname, username, password, email, contact_number, address, role, status) VALUES(?, ?, ?, ?, ?, ?, ?, 'APPROVED')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, fullname);
            pstmt.setString(2, username);
            pstmt.setString(3, password);
            pstmt.setString(4, email);
            pstmt.setString(5, contactNumber);
            pstmt.setString(6, address);
            pstmt.setString(7, role);
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int newId = keys.getInt(1);
                    logDao.logAction(adminId, "USER_CREATED",
                            "Admin " + adminId + " created account for User " + newId
                                    + " (" + username + ") as " + role);
                    return newId;
                }
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                System.out.println(" -> [ERROR] Username already exists.");
            } else {
                System.err.println("Error creating user: " + e.getMessage());
            }
        }
        return -1;
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

    public List<User> getUsers(String statusFilter) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users"
                + (statusFilter != null ? " WHERE status = ?" : "") + " ORDER BY user_id";

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

    public boolean updateStatus(int adminId, int userId, String status) {
        String sql = "UPDATE users SET status = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            pstmt.setInt(2, userId);
            boolean ok = pstmt.executeUpdate() > 0;
            if (ok) {
                if ("APPROVED".equalsIgnoreCase(status)) {
                    logDao.logAction(adminId, "USER_APPROVED",
                            "Admin " + adminId + " approved User " + userId);
                } else {
                    logDao.logAction(adminId, "USER_UPDATED",
                            "Admin " + adminId + " set status of User " + userId + " to " + status);
                }
            }
            return ok;
        } catch (SQLException e) {
            System.err.println("Error updating status: " + e.getMessage());
            return false;
        }
    }

    public boolean updateRole(int adminId, int userId, String role) {
        String sql = "UPDATE users SET role = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, role);
            pstmt.setInt(2, userId);
            boolean ok = pstmt.executeUpdate() > 0;
            if (ok) logDao.logAction(adminId, "USER_UPDATED",
                    "Admin " + adminId + " changed role of User " + userId + " to " + role);
            return ok;
        } catch (SQLException e) {
            System.err.println("Error updating role: " + e.getMessage());
            return false;
        }
    }
}
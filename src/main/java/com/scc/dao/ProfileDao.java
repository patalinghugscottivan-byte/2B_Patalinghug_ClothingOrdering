package com.scc.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProfileDao {

    public record Profile(String fullName, String email, String contact, String address) {
    }

    private final ActivityLogDao logDao = new ActivityLogDao();

    private static String find(List<String> cols, String... candidates) {
        for (String c : candidates) {
            for (String col : cols) {
                if (col.equalsIgnoreCase(c)) return col;
            }
        }
        return null;
    }

    private List<String> userColumns(Connection conn) throws SQLException {
        List<String> cols = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(users)")) {
            while (rs.next()) cols.add(rs.getString("name"));
        }
        return cols;
    }

    private String sel(String col, String alias) {
        return col == null ? "'' AS " + alias : "COALESCE(" + col + ", '') AS " + alias;
    }

    public Profile getProfile(int userId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            List<String> cols = userColumns(conn);
            String name = find(cols, "full_name", "fullname", "name");
            String email = find(cols, "email");
            String contact = find(cols, "contact_number", "contact", "contact_no", "phone");
            String address = find(cols, "address");

            String sql = "SELECT " + sel(name, "f") + ", " + sel(email, "e") + ", "
                    + sel(contact, "c") + ", " + sel(address, "a") + " FROM users WHERE user_id = ?";
            try (PreparedStatement p = conn.prepareStatement(sql)) {
                p.setInt(1, userId);
                try (ResultSet rs = p.executeQuery()) {
                    if (rs.next()) {
                        return new Profile(rs.getString("f"), rs.getString("e"),
                                rs.getString("c"), rs.getString("a"));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading profile: " + e.getMessage());
        }
        return null;
    }

    public boolean updateProfile(int userId, Profile np) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            List<String> cols = userColumns(conn);
            String name = find(cols, "full_name", "fullname", "name");
            String email = find(cols, "email");
            String contact = find(cols, "contact_number", "contact", "contact_no", "phone");
            String address = find(cols, "address");

            List<String> sets = new ArrayList<>();
            List<String> values = new ArrayList<>();
            if (name != null) { sets.add(name + " = ?"); values.add(np.fullName()); }
            if (email != null) { sets.add(email + " = ?"); values.add(np.email()); }
            if (contact != null) { sets.add(contact + " = ?"); values.add(np.contact()); }
            if (address != null) { sets.add(address + " = ?"); values.add(np.address()); }
            if (sets.isEmpty()) return false;

            String sql = "UPDATE users SET " + String.join(", ", sets) + " WHERE user_id = ?";
            try (PreparedStatement p = conn.prepareStatement(sql)) {
                int i = 1;
                for (String v : values) p.setString(i++, v);
                p.setInt(i, userId);
                boolean ok = p.executeUpdate() > 0;
                if (ok) logDao.logAction(userId, "PROFILE_UPDATED", "User #" + userId + " updated own profile");
                return ok;
            }
        } catch (SQLException e) {
            System.err.println("Error updating profile: " + e.getMessage());
            return false;
        }
    }
}
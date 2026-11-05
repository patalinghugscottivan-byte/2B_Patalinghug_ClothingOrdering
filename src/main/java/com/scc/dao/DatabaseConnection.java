package com.scc.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String DB_URL = "jdbc:sqlite:CLOTHING ORDERING SYSTEM.db";

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        try (PreparedStatement ps = conn.prepareStatement("PRAGMA foreign_keys = ON;")) {
            ps.execute();
        }
        return conn;
    }
}
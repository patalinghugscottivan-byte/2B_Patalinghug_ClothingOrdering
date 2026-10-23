package com.scc.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initialize() {
        String usersTable = """
            CREATE TABLE IF NOT EXISTS users (
                user_id        INTEGER PRIMARY KEY AUTOINCREMENT,
                fullname       TEXT NOT NULL,
                username       TEXT NOT NULL UNIQUE,
                password       TEXT NOT NULL,
                email          TEXT NOT NULL,
                contact_number TEXT NOT NULL,
                address        TEXT NOT NULL,
                role           TEXT NOT NULL,
                status         TEXT NOT NULL DEFAULT 'PENDING',
                date_created   TEXT DEFAULT CURRENT_TIMESTAMP
            );
            """;

        // TODO: dugangi diri ang uban tables gikan sa ERD (products, orders, etc.)

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(usersTable);
            System.out.println("-> Database initialized.");
        } catch (SQLException e) {
            throw new RuntimeException("Database initialization failed: " + e.getMessage(), e);
        }
    }
}
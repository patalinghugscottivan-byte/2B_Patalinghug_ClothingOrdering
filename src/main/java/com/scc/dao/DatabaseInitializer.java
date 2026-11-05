package com.scc.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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
                date_created   TEXT DEFAULT (datetime('now', 'localtime'))
            );
            """;

        String productsTable = """
            CREATE TABLE IF NOT EXISTS products (
                product_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                product_name   TEXT NOT NULL,
                category       TEXT NOT NULL,
                description    TEXT,
                size           TEXT NOT NULL,
                color          TEXT NOT NULL,
                price          REAL NOT NULL CHECK (price > 0),
                stock_quantity INTEGER NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
                reorder_level  INTEGER NOT NULL DEFAULT 5,
                status         TEXT NOT NULL DEFAULT 'ACTIVE',
                date_added     TEXT DEFAULT (datetime('now', 'localtime'))
            );
            """;

        String ordersTable = """
            CREATE TABLE IF NOT EXISTS orders (
                order_id         INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id          INTEGER NOT NULL,
                order_date       TEXT DEFAULT (datetime('now', 'localtime')),
                total_amount     REAL NOT NULL,
                payment_method   TEXT NOT NULL,
                delivery_address TEXT NOT NULL,
                order_status     TEXT NOT NULL DEFAULT 'PENDING',
                processed_by     INTEGER,
                FOREIGN KEY (user_id) REFERENCES users(user_id),
                FOREIGN KEY (processed_by) REFERENCES users(user_id)
            );
            """;

        String orderItemsTable = """
            CREATE TABLE IF NOT EXISTS order_items (
                order_item_id INTEGER PRIMARY KEY AUTOINCREMENT,
                order_id      INTEGER NOT NULL,
                product_id    INTEGER NOT NULL,
                quantity      INTEGER NOT NULL CHECK (quantity > 0),
                unit_price    REAL NOT NULL,
                subtotal      REAL NOT NULL,
                FOREIGN KEY (order_id) REFERENCES orders(order_id),
                FOREIGN KEY (product_id) REFERENCES products(product_id)
            );
            """;

        String activityLogsTable = """
            CREATE TABLE IF NOT EXISTS activity_logs (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id     INTEGER NOT NULL,
                action_type TEXT NOT NULL,
                description TEXT NOT NULL,
                timestamp   TEXT DEFAULT (datetime('now', 'localtime')),
                FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
            );
            """;

        String activityLogsIndex =
                "CREATE INDEX IF NOT EXISTS idx_activity_logs_user_id ON activity_logs(user_id);";

        String[] statements = {
                usersTable, productsTable, ordersTable, orderItemsTable,
                activityLogsTable, activityLogsIndex
        };

        try (Connection conn = DatabaseConnection.getConnection()) {
            for (String sql : statements) {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.execute();
                }
            }
            System.out.println("-> Database initialized.");
        } catch (SQLException e) {
            throw new RuntimeException("Database initialization failed: " + e.getMessage(), e);
        }
    }
}
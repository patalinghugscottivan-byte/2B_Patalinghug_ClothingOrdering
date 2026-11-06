package com.scc.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {


    public static void initialize() {
        String users = """
                CREATE TABLE IF NOT EXISTS users (
                    user_id        INTEGER PRIMARY KEY AUTOINCREMENT,
                    full_name      TEXT NOT NULL,
                    username       TEXT NOT NULL UNIQUE,
                    password       TEXT NOT NULL,
                    email          TEXT,
                    contact_number TEXT,
                    address        TEXT,
                    role           TEXT NOT NULL DEFAULT 'CUSTOMER',
                    status         TEXT NOT NULL DEFAULT 'PENDING',
                    created_at     TEXT DEFAULT CURRENT_TIMESTAMP
                )
                """;

        String products = """
                CREATE TABLE IF NOT EXISTS products (
                    product_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                    product_name   TEXT NOT NULL,
                    category       TEXT,
                    description    TEXT,
                    size           TEXT,
                    color          TEXT,
                    price          REAL NOT NULL,
                    stock_quantity INTEGER NOT NULL DEFAULT 0,
                    reorder_level  INTEGER NOT NULL DEFAULT 0,
                    status         TEXT NOT NULL DEFAULT 'ACTIVE'
                )
                """;

        String orders = """
                CREATE TABLE IF NOT EXISTS orders (
                    order_id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id          INTEGER NOT NULL,
                    total_amount     REAL NOT NULL,
                    payment_method   TEXT,
                    delivery_address TEXT,
                    order_status     TEXT NOT NULL DEFAULT 'PENDING',
                    processed_by     INTEGER,
                    order_date       TEXT DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(user_id)
                )
                """;

        String orderItems = """
                CREATE TABLE IF NOT EXISTS order_items (
                    order_item_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    order_id      INTEGER NOT NULL,
                    product_id    INTEGER NOT NULL,
                    quantity      INTEGER NOT NULL,
                    unit_price    REAL NOT NULL,
                    subtotal      REAL NOT NULL,
                    FOREIGN KEY (order_id) REFERENCES orders(order_id),
                    FOREIGN KEY (product_id) REFERENCES products(product_id)
                )
                """;

        String activityLogs = """
                CREATE TABLE IF NOT EXISTS activity_logs (
                    log_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id     INTEGER,
                    action_type TEXT NOT NULL,
                    description TEXT,
                    timestamp   TEXT DEFAULT CURRENT_TIMESTAMP
                )
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(users);
            st.execute(products);
            st.execute(orders);
            st.execute(orderItems);
            st.execute(activityLogs);
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }
}
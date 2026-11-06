package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.OrderDao;
import com.scc.dao.ProductDao;
import com.scc.dao.UserDao;
import com.scc.model.ActivityLog;
import com.scc.model.Product;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final UserDao userDao;
    private final ActivityLogDao logDao = new ActivityLogDao();
    private final ProductDao productDao = new ProductDao();
    private final OrderDao orderDao = new OrderDao();
    private final Scanner sc;

    public AdminMenu(UserDao userDao, Scanner sc) {
        this.userDao = userDao;
        this.sc = sc;
    }

    public void display(User admin) {
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("\n==============================");
            System.out.println("           ADMIN MENU ");
            System.out.println(" User: " + admin.username());
            System.out.println("==============================");
            System.out.println("1. Manage Users");
            System.out.println("2. Manage Products");
            System.out.println("3. Manage Orders");
            System.out.println("4. Reports");
            System.out.println("5. View System Audit Logs");
            System.out.println("6. Edit My Profile");
            System.out.println("0. Logout");
            System.out.print("Enter choice: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> manageUsers(admin);
                case "2" -> manageProducts(admin);
                case "3" -> ManageOrders.run(sc, orderDao, admin);
                case "4" -> reports();
                case "5" -> auditLogs();
                case "6" -> editProfile();
                case "0" -> {
                    if (confirm("Are you sure you want to logout?")) {
                        System.out.println("You have been logged out. Goodbye, " + admin.username() + "!");
                        loggedIn = false;
                    }
                }
                default -> System.out.println("-> Invalid choice. Try again.");
            }
        }
    }

    private void manageUsers(User admin) {
        while (true) {
            System.out.println("\n===============================");
            System.out.println("          MANAGE USERS");
            System.out.println("===============================");
            System.out.println("[1] Add New User\n[2] View All Users\n[3] Approve Pending Accounts" +
                    "\n[4] Change Role\n[5] Activate / Deactivate Account\n[0] Back");
            System.out.print("Enter Choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> addUser(admin);
                case "2" -> viewAllUsers();
                case "3" -> approvePendingAccounts(admin);
                case "4" -> changeRole(admin);
                case "5" -> toggleAccountStatus(admin);
                case "0" -> {
                    return;
                }
                default -> System.out.println("-> Invalid choice! 0 to 5 only.");
            }
        }
    }

    private void addUser(User admin) {
        System.out.println("\n--- ADD NEW USER ---");

        String fullname = InputUtil.readRequired(sc, "Full name: ");

        String username;
        while (true) {
            System.out.print("Username (4-20 letters/numbers): ");
            username = sc.nextLine().trim();
            if (username.matches("[A-Za-z0-9_]{4,20}")) break;
            System.out.println(" -> [ERROR] Use 4-20 letters, numbers, or underscore only.");
        }

        String password;
        while (true) {
            System.out.print("Password (at least 6 characters): ");
            password = sc.nextLine();
            if (password.length() >= 6) break;
            System.out.println(" -> [ERROR] Password is too short.");
        }

        String email;
        while (true) {
            System.out.print("Email: ");
            email = sc.nextLine().trim();
            if (email.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) break;
            System.out.println(" -> [ERROR] Invalid email format (example: name@gmail.com).");
        }

        String contact;
        while (true) {
            System.out.print("Contact number (11 digits): ");
            contact = sc.nextLine().trim();
            if (contact.matches("\\d{11}")) break;
            System.out.println(" -> [ERROR] Must be exactly 11 digits.");
        }

        String address = InputUtil.readRequired(sc, "Address: ");

        String role = readValidRole();
        if (role == null) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }

        int newId = userDao.createUser(admin.user_id(), fullname, username, password,
                email, contact, address, role);
        if (newId > 0) {
            System.out.println(" -> User #" + newId + " (" + username + ") created as " + role + ".");
        } else {
            System.out.println(" -> [ERROR] Failed to create user.");
        }
    }

    private void displayUserTable(List<User> users, String title) {
        System.out.println("\n====================== " + title + " ======================");

        if (users.isEmpty()) {
            System.out.println("\n-> No Records.");
            return;
        }

        String border = "+------+------------------------------+----------------+------------+";
        String format = "| %-4s | %-28s | %-14s | %-10s |\n";

        System.out.println();
        System.out.println(border);
        System.out.printf(format, "ID", "Username", "Role", "Status");
        System.out.println(border);

        for (User u : users) {
            System.out.printf(format, u.user_id(), u.username(), u.role(), u.status());
        }

        System.out.println(border);
    }

    private void viewAllUsers() {
        boolean running = true;
        displayUserTable(userDao.getUsers(null), "ALL REGISTERED USERS");
        while (running) {
            System.out.print("Filter ([1] PENDING  | [2] APPROVED\n" +
                    "        [3] INACTIVE | [0] Back): ");
            String chooseFilter = sc.nextLine().trim();

            switch (chooseFilter) {
                case "1" -> viewUsers("PENDING");
                case "2" -> viewUsers("APPROVED");
                case "3" -> viewUsers("INACTIVE");
                case "0" -> running = false;
                default -> System.out.println("Please Enter 0-3.");
            }
        }
    }

    private void viewUsers(String filterStatus) {
        if (filterStatus.equals("PENDING")) {
            displayUserTable(userDao.getUsers("PENDING"), "PENDING USERS");
        } else if (filterStatus.equals("APPROVED")) {
            displayUserTable(userDao.getUsers("APPROVED"), "APPROVED USERS");
        } else {
            displayUserTable(userDao.getUsers("INACTIVE"), "INACTIVE USERS");
        }
    }

    private void approvePendingAccounts(User admin) {
        List<User> pending = userDao.getUsers("PENDING");
        displayUserTable(pending, "PENDING APPROVALS");

        if (pending.isEmpty()) {
            return;
        }

        int id = readInput("Enter User ID to approve (0 to cancel): ");

        if (id == 0) {
            System.out.println("-> Cancelled.");
            return;
        }

        User target = pending.stream()
                .filter(u -> u.user_id() == id)
                .findFirst()
                .orElse(null);

        if (target == null) {
            System.out.println(" -> [ERROR] User ID not found in the pending list.");
            return;
        }

        if (userDao.updateStatus(admin.user_id(), id, "APPROVED")) {
            System.out.println(" -> " + target.username() + " has been APPROVED.");
        } else {
            System.out.println(" -> [ERROR] Failed to approve user.");
        }
    }

    private void changeRole(User admin) {
        List<User> users = userDao.getUsers(null);
        displayUserTable(users, "SELECT USER TO UPDATE");

        if (users.isEmpty()) {
            return;
        }

        int id = readInput("Enter user ID to change role (0 to cancel): ");

        if (id == 0) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }

        boolean exists = users.stream().anyMatch(u -> u.user_id() == id);
        if (!exists) {
            System.out.println(" -> [ERROR] User ID not found.");
            return;
        }

        if (id == admin.user_id()) {
            System.out.println(" -> [WARNING] Modifying current active Admin is Restricted.");
            return;
        }

        String newRole = readValidRole();
        if (newRole == null) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }

        if (!confirm("Update user " + id + " to role = " + newRole + "?")) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }

        if (userDao.updateRole(admin.user_id(), id, newRole)) {
            System.out.println(" -> Role updated to " + newRole + ".");
        } else {
            System.out.println(" -> [ERROR] Failed to change role.");
        }
    }

    private String readValidRole() {
        while (true) {
            System.out.print("Enter Role (ADMIN/STAFF/CUSTOMER [0 to cancel]): ");
            String role = sc.nextLine().trim().toUpperCase();

            if (role.equals("0") || role.equals("CANCEL")) {
                return null;
            }

            if (role.equals("ADMIN") || role.equals("STAFF") || role.equals("CUSTOMER")) {
                return role;
            }

            System.out.println(" -> [ERROR] Invalid Role.");
        }
    }

    private void toggleAccountStatus(User admin) {
        List<User> users = userDao.getUsers(null);
        displayUserTable(users, "ACTIVATE / DEACTIVATE");

        if (users.isEmpty()) {
            return;
        }

        int id = readInput("Enter user ID (0 to cancel): ");

        if (id == 0) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }

        User target = users.stream()
                .filter(u -> u.user_id() == id)
                .findFirst()
                .orElse(null);

        if (target == null) {
            System.out.println(" -> [ERROR] User ID not found.");
            return;
        }

        if (id == admin.user_id()) {
            System.out.println(" -> [WARNING] You cannot deactivate your own account.");
            return;
        }

        if ("PENDING".equals(target.status())) {
            System.out.println(" -> [ERROR] Account is still pending. Approve it first.");
            return;
        }

        String newStatus = "APPROVED".equals(target.status()) ? "INACTIVE" : "APPROVED";
        String action = newStatus.equals("INACTIVE") ? "deactivate" : "activate";

        if (!confirm("Are you sure you want to " + action + " " + target.username() + "?")) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }

        if (userDao.updateStatus(admin.user_id(), id, newStatus)) {
            System.out.println(" -> " + target.username() + " is now " + newStatus + ".");
        } else {
            System.out.println(" -> [ERROR] Failed to update status.");
        }
    }

    private void manageProducts(User admin) {
        while (true) {
            System.out.println("\n===============================");
            System.out.println("        MANAGE PRODUCTS");
            System.out.println("===============================");
            System.out.println("[1] Add New Product\n[2] View / Search / Filter Products" +
                    "\n[3] Update Product Price / Stock\n[4] Discontinue Product\n[0] Back");
            System.out.print("Enter Choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> addProduct(admin);
                case "2" -> ProductView.browse(sc, productDao);
                case "3" -> updateProduct(admin);
                case "4" -> discontinueProduct(admin);
                case "0" -> {
                    return;
                }
                default -> System.out.println("-> Invalid choice! 0 to 4 only.");
            }
        }
    }

    private void addProduct(User admin) {
        System.out.println("\n--- ADD NEW PRODUCT ---");
        String name = InputUtil.readRequired(sc, "Product name: ");
        String category = InputUtil.readRequired(sc, "Category (e.g. Tops, Bottoms): ");
        String description = InputUtil.readRequired(sc, "Description: ");
        String size = InputUtil.readRequired(sc, "Size (e.g. S, M, L, XL): ");
        String color = InputUtil.readRequired(sc, "Color: ");
        double price = InputUtil.readDouble(sc, "Price (P): ", 0.01);
        int stock = InputUtil.readInt(sc, "Stock quantity: ", 0);
        int reorder = InputUtil.readInt(sc, "Reorder level: ", 0);

        int id = productDao.addProduct(admin.user_id(), name, category, description,
                size, color, price, stock, reorder);
        if (id > 0) {
            System.out.println(" -> Product #" + id + " added.");
        } else {
            System.out.println(" -> [ERROR] Failed to add product.");
        }
    }

    private void updateProduct(User admin) {
        ProductView.print(productDao.getAllProducts());
        int id = InputUtil.readInt(sc, "Enter Product ID (0 to cancel): ", 0);
        if (id == 0) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }
        Product p = productDao.getProductById(id);
        if (p == null) {
            System.out.println(" -> [ERROR] Product ID not found.");
            return;
        }

        System.out.print("Update [1] Price | [2] Stock | [0] Cancel: ");
        String c = sc.nextLine().trim();
        switch (c) {
            case "1" -> {
                double price = InputUtil.readDouble(sc, "New price (P): ", 0.01);
                System.out.println(productDao.updatePrice(admin.user_id(), id, price)
                        ? " -> Price updated." : " -> [ERROR] Failed to update price.");
            }
            case "2" -> {
                int stock = InputUtil.readInt(sc, "New stock quantity: ", 0);
                System.out.println(productDao.updateStock(admin.user_id(), id, stock)
                        ? " -> Stock updated." : " -> [ERROR] Failed to update stock.");
            }
            case "0" -> System.out.println(" -> Operation Cancelled.");
            default -> System.out.println(" -> Invalid choice.");
        }
    }

    private void discontinueProduct(User admin) {
        ProductView.print(productDao.getAllProducts());
        int id = InputUtil.readInt(sc, "Enter Product ID to discontinue (0 to cancel): ", 0);
        if (id == 0) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }
        Product p = productDao.getProductById(id);
        if (p == null) {
            System.out.println(" -> [ERROR] Product ID not found.");
            return;
        }
        if (!confirm("Discontinue " + p.productName() + "?")) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }
        System.out.println(productDao.discontinueProduct(admin.user_id(), id)
                ? " -> Product discontinued." : " -> [ERROR] Already discontinued or failed.");
    }

    private void reports() {
        while (true) {
            System.out.println("\n===============================");
            System.out.println("            REPORTS");
            System.out.println("===============================");
            System.out.println("[1] Sales Report (Daily / Monthly)\n[2] Low-Stock Report\n[0] Back");
            System.out.print("Enter Choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> System.out.println("\n-> Sales report coming soon...");
                case "2" -> ProductView.print(productDao.getLowStock());
                case "0" -> {
                    return;
                }
                default -> System.out.println("-> Invalid choice! 0 to 2 only.");
            }
        }
    }

    private void auditLogs() {
        while (true) {
            System.out.println("\n===============================");
            System.out.println("      SYSTEM AUDIT LOGS");
            System.out.println("===============================");
            System.out.println("[1] View All System Logs\n[2] Filter Logs by User ID\n[0] Back");
            System.out.print("Enter Choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> printLogs(logDao.getAllLogs());
                case "2" -> {
                    int userId = readInput("Enter User ID: ");
                    printLogs(logDao.getLogsByUserId(userId));
                }
                case "0" -> {
                    return;
                }
                default -> System.out.println("-> Invalid choice! 0 to 2 only.");
            }
        }
    }

    private void printLogs(List<ActivityLog> logs) {
        String fmt = "%-5s | %-8s | %-18s | %-35s | %-20s%n";
        String line = "-".repeat(100);
        System.out.println("\n" + line);
        System.out.printf(fmt, "ID", "USER ID", "ACTION", "DESCRIPTION", "TIMESTAMP");
        System.out.println(line);

        if (logs.isEmpty()) {
            System.out.println("-> No logs found.");
            System.out.println(line);
            return;
        }

        for (ActivityLog l : logs) {
            String desc = l.description().length() > 35
                    ? l.description().substring(0, 32) + "..."
                    : l.description();
            System.out.printf(fmt, l.id(), l.userId(), l.actionType(), desc, l.timestamp());
        }
        System.out.println(line);
    }

    private void editProfile() {
        System.out.println("\n=== EDIT MY PROFILE ===");
    }

    private int readInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();

            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println(" -> [ERROR] Please enter a valid numeric ID.");
            }
        }
    }

    private boolean confirm(String message) {
        System.out.print(message + " (Y/N): ");
        return sc.nextLine().trim().equalsIgnoreCase("Y");
    }
}
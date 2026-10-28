package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final UserDao userDao;
    private final Scanner sc;

    public AdminMenu(UserDao userDao, Scanner sc) {
        this.userDao = userDao;
        this.sc = sc;
    }

    // ===================== MAIN ADMIN MENU =====================

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
            System.out.println("5. View Audit Logs");
            System.out.println("6. Edit My Profile");
            System.out.println("0. Logout");
            System.out.print("Enter choice: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> manageUsers(admin);
                case "2" -> manageProducts();
                case "3" -> manageOrders();
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

    // ===================== MANAGE USERS =====================

    private void manageUsers(User admin) {
        while (true) {
            System.out.println("\n===============================");
            System.out.println("          MANAGE USERS");
            System.out.println("===============================");
            System.out.println("[1] View All Users\n[2] Approve Pending Accounts\n[3] Change Role" +
                    "\n[4] Activate / Deactivate Account\n[0] Back");
            System.out.print("Enter Choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> viewAllUsers();
                case "2" -> approvePendingAccounts();
                case "3" -> changeRole(admin);
                case "4" -> toggleAccountStatus(admin);
                case "0" -> {
                    return;
                }
                default -> System.out.println("-> Invalid choice! 0 to 4 only.");
            }
        }
    }

    // ---------- Table display ----------

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

    // ---------- 1. View all ----------

    private void viewAllUsers() {
        boolean running = true;
        displayUserTable(userDao.getUsers(null), "ALL REGISTERED USERS");
        while(running){
            System.out.print("Filter ([1] PENDING  | [2] APPROVED\n" +
                    "        [3] INACTIVE | [0] Back): ");
            String chooseFilter = sc.nextLine();

            switch (chooseFilter){
                case "1": viewUsers("PENDING");
                    break;
                case "2": viewUsers("APPROVED");
                    break;
                case "3": viewUsers("INACTIVE");
                    break;
                case "0": running = false;
                    break;
                default:
                    System.out.println("Please Enter 0-2.");
            }
        }
    }

    private void viewUsers(String filterStatus){
        if(filterStatus.equals("PENDING")){
            displayUserTable(userDao.getUsers("PENDING"), "PENDING USERS");
        }else if(filterStatus.equals("APPROVED")){
            displayUserTable(userDao.getUsers("APPROVED"), "APPROVED USERS");
        }else{
            displayUserTable(userDao.getUsers("INACTIVE"), "INACTIVE USERS");
        }
    }


    // ---------- 2. Approve pending ----------

    private void approvePendingAccounts() {
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

        if (userDao.updateStatus(id, "APPROVED")) {
            System.out.println(" -> " + target.username() + " has been APPROVED.");
        } else {
            System.out.println(" -> [ERROR] Failed to approve user.");
        }
    }

    // ---------- 3. Change role ----------

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

        if (userDao.updateRole(id, newRole)) {
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

    // ---------- 4. Activate / Deactivate ----------

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

        if (userDao.updateStatus(id, newStatus)) {
            System.out.println(" -> " + target.username() + " is now " + newStatus + ".");
        } else {
            System.out.println(" -> [ERROR] Failed to update status.");
        }
    }

    // ===================== MANAGE PRODUCTS =====================

    private void manageProducts() {
        while (true) {
            System.out.println("\n===============================");
            System.out.println("        MANAGE PRODUCTS");
            System.out.println("===============================");
            System.out.println("[1] Add New Product\n[2] View / Search / Filter Products" +
                    "\n[3] Update Product Details, Price, Stock\n[4] Discontinue Product\n[0] Back");
            System.out.print("Enter Choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> System.out.println("\n-> Add product coming soon...");
                case "2" -> System.out.println("\n-> Search products coming soon...");
                case "3" -> System.out.println("\n-> Update product coming soon...");
                case "4" -> System.out.println("\n-> Discontinue product coming soon...");
                case "0" -> {
                    return;
                }
                default -> System.out.println("-> Invalid choice! 0 to 4 only.");
            }
        }
    }

    // ===================== MANAGE ORDERS =====================

    private void manageOrders() {
        while (true) {
            System.out.println("\n===============================");
            System.out.println("         MANAGE ORDERS");
            System.out.println("===============================");
            System.out.println("[1] View All Orders\n[2] Search Orders" +
                    "\n[3] Update Order Status\n[4] Cancel Order\n[0] Back");
            System.out.print("Enter Choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> System.out.println("\n-> View orders coming soon...");
                case "2" -> System.out.println("\n-> Search orders coming soon...");
                case "3" -> System.out.println("\n-> Update status coming soon...");
                case "4" -> System.out.println("\n-> Cancel order coming soon...");
                case "0" -> {
                    return;
                }
                default -> System.out.println("-> Invalid choice! 0 to 4 only.");
            }
        }
    }

    // ===================== REPORTS =====================

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
                case "2" -> System.out.println("\n-> Low-stock report coming soon...");
                case "0" -> {
                    return;
                }
                default -> System.out.println("-> Invalid choice! 0 to 2 only.");
            }
        }
    }

    // ===================== AUDIT LOGS =====================

    private void auditLogs() {
        while (true) {
            System.out.println("\n===============================");
            System.out.println("           AUDIT LOGS");
            System.out.println("===============================");
            System.out.println("[1] User Activity Log\n[2] Order & Inventory Audit Log\n[0] Back");
            System.out.print("Enter Choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> System.out.println("\n-> User activity log coming soon...");
                case "2" -> System.out.println("\n-> Order & inventory log coming soon...");
                case "0" -> {
                    return;
                }
                default -> System.out.println("-> Invalid choice! 0 to 2 only.");
            }
        }
    }

    // ===================== COMING SOON =====================

    private void editProfile() {
        System.out.println("\n=== EDIT MY PROFILE ===");
    }

    // ===================== HELPERS =====================

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
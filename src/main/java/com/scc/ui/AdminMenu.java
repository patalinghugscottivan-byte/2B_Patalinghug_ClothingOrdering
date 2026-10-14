package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class AdminMenu {
    private final UserDao userDao;
    private  final Scanner sc;

    public AdminMenu(UserDao userdao, Scanner sc){
        this.userDao = userdao;
        this.sc = sc;
    }

    public void display (User admin){
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("\n==========================");
            System.out.println(" ADMIN MENU");
            System.out.println("==========================");
            System.out.println("1. Manage Users");
            System.out.println("2. Manage Products");
            System.out.println("3. Manage Orders");
            System.out.println("4. Reports");
            System.out.println("5. View Audit Logs");
            System.out.println("6. Edit My Profile");
            System.out.println("7. Logout");
            System.out.print("Choose an option: ");
            String adminChoice = sc.nextLine().trim();

            switch (adminChoice) {

                case "1": {
                    boolean back = false;
                    while (!back) {
                        System.out.println("\n--- MANAGE USERS ---");
                        System.out.println("1. View all users");
                        System.out.println("2. Approve pending accounts");
                        System.out.println("3. Change role");
                        System.out.println("4. Activate / Deactivate account");
                        System.out.println("0. Back");
                        System.out.print("Choose an option: ");
                        String sub = sc.nextLine().trim();
                        switch (sub) {
                            case "1": System.out.println("View all users coming soon..."); break;
                            case "2": System.out.println("Approve pending accounts coming soon..."); break;
                            case "3": System.out.println("Change role coming soon..."); break;
                            case "4": System.out.println("Activate / Deactivate coming soon..."); break;
                            case "0": back = true; break;
                            default: System.out.println("Invalid option! Please choose 0-4.");
                        }
                    }
                    break;
                }

                case "2": {
                    boolean back = false;
                    while (!back) {
                        System.out.println("\n--- MANAGE PRODUCTS ---");
                        System.out.println("1. Add new product");
                        System.out.println("2. View / search / filter products");
                        System.out.println("3. Update product details, price, stock");
                        System.out.println("4. Discontinue product");
                        System.out.println("0. Back");
                        System.out.print("Choose an option: ");
                        String sub = sc.nextLine().trim();
                        switch (sub) {
                            case "1": System.out.println("Add product coming soon..."); break;
                            case "2": System.out.println("Search products coming soon..."); break;
                            case "3": System.out.println("Update product coming soon..."); break;
                            case "4": System.out.println("Discontinue product coming soon..."); break;
                            case "0": back = true; break;
                            default: System.out.println("Invalid option! Please choose 0-4.");
                        }
                    }
                    break;
                }

                case "3": {
                    boolean back = false;
                    while (!back) {
                        System.out.println("\n--- MANAGE ORDERS ---");
                        System.out.println("1. View all orders");
                        System.out.println("2. Search orders");
                        System.out.println("3. Update order status");
                        System.out.println("4. Cancel order");
                        System.out.println("0. Back");
                        System.out.print("Choose an option: ");
                        String sub = sc.nextLine().trim();
                        switch (sub) {
                            case "1": System.out.println("View orders coming soon..."); break;
                            case "2": System.out.println("Search orders coming soon..."); break;
                            case "3": System.out.println("Update status coming soon..."); break;
                            case "4": System.out.println("Cancel order coming soon..."); break;
                            case "0": back = true; break;
                            default: System.out.println("Invalid option! Please choose 0-4.");
                        }
                    }
                    break;
                }

                case "4": {
                    boolean back = false;
                    while (!back) {
                        System.out.println("\n--- REPORTS ---");
                        System.out.println("1. Sales Report (Daily / Monthly)");
                        System.out.println("2. Low-Stock Report");
                        System.out.println("0. Back");
                        System.out.print("Choose an option: ");
                        String sub = sc.nextLine().trim();
                        switch (sub) {
                            case "1": System.out.println("Sales report coming soon..."); break;
                            case "2": System.out.println("Low-stock report coming soon..."); break;
                            case "0": back = true; break;
                            default: System.out.println("Invalid option! Please choose 0-2.");
                        }
                    }
                    break;
                }

                case "5": {
                    boolean back = false;
                    while (!back) {
                        System.out.println("\n--- AUDIT LOGS ---");
                        System.out.println("1. User Activity Log");
                        System.out.println("2. Order & Inventory Audit Log");
                        System.out.println("0. Back");
                        System.out.print("Choose an option: ");
                        String sub = sc.nextLine().trim();
                        switch (sub) {
                            case "1": System.out.println("User activity log coming soon..."); break;
                            case "2": System.out.println("Order & inventory log coming soon..."); break;
                            case "0": back = true; break;
                            default: System.out.println("Invalid option! Please choose 0-2.");
                        }
                    }
                    break;
                }

                case "6":
                    System.out.println("Edit profile coming soon...");
                    break;

                case "7": {
                    String confirm;
                    while (true) {
                        System.out.print("Are you sure you want to logout? (Y/N): ");
                        confirm = sc.nextLine().trim().toUpperCase();
                        if (confirm.equals("Y") || confirm.equals("N")) break;
                        System.out.println("Invalid input. Please enter Y or N.");
                    }
                    if (confirm.equals("Y")) {
                        loggedIn = false;
                        System.out.println("Logged out successfully.");
                    }
                    break;
                }

                default:
                    System.out.println("Invalid option! Please choose 1-7.");
            }
        }
    }


}

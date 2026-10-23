package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class CustomerMenu {
    private final UserDao userDao;
    private final Scanner sc;

    public CustomerMenu(UserDao userDao, Scanner sc) {
        this.userDao = userDao;
        this.sc = sc;
    }

    public void display(User customer) {
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("\n==========================");
            System.out.println(" CUSTOMER MENU");
            System.out.println("==========================");
            System.out.println("1. Products");
            System.out.println("2. Orders");
            System.out.println("3. Edit My Profile");
            System.out.println("4. Logout");
            System.out.print("Choose an option: ");
            String choice = sc.nextLine().trim();

            switch (choice) {

                case "1": {
                    boolean back = false;
                    while (!back) {
                        System.out.println("\n--- PRODUCTS ---");
                        System.out.println("1. Browse products");
                        System.out.println("2. Search products");
                        System.out.println("3. Filter products");
                        System.out.println("0. Back");
                        System.out.print("Choose an option: ");
                        String sub = sc.nextLine().trim();
                        switch (sub) {
                            case "1": System.out.println("Browse products coming soon..."); break;
                            case "2": System.out.println("Search products coming soon..."); break;
                            case "3": System.out.println("Filter products coming soon..."); break;
                            case "0": back = true; break;
                            default: System.out.println("Invalid option! Please choose 0-3.");
                        }
                    }
                    break;
                }

                case "2": {
                    boolean back = false;
                    while (!back) {
                        System.out.println("\n--- ORDERS ---");
                        System.out.println("1. Place order");
                        System.out.println("2. View my orders");
                        System.out.println("3. Cancel my order (PENDING only)");
                        System.out.println("0. Back");
                        System.out.print("Choose an option: ");
                        String sub = sc.nextLine().trim();
                        switch (sub) {
                            case "1": System.out.println("Place order coming soon..."); break;
                            case "2": System.out.println("View my orders coming soon..."); break;
                            case "3": System.out.println("Cancel order coming soon..."); break;
                            case "0": back = true; break;
                            default: System.out.println("Invalid option! Please choose 0-3.");
                        }
                    }
                    break;
                }

                case "3":
                    System.out.println("Edit profile coming soon...");
                    break;

                case "4": {
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
                    System.out.println("Invalid option! Please choose 1-4.");
            }
        }
    }
}
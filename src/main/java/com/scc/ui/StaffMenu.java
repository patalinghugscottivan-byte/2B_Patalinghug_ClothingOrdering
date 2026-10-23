package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class StaffMenu {
    private final UserDao userDao;
    private final Scanner sc;

    public StaffMenu(UserDao userDao, Scanner sc) {
        this.userDao = userDao;
        this.sc = sc;
    }

    public void display(User staff) {
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("\n==========================");
            System.out.println(" STAFF MENU");
            System.out.println("==========================");
            System.out.println("1. Products");
            System.out.println("2. Orders");
            System.out.println("3. Reports");
            System.out.println("4. Logout");
            System.out.print("Choose an option: ");
            String choice = sc.nextLine().trim();

            switch (choice) {

                case "1": {
                    boolean back = false;
                    while (!back) {
                        System.out.println("\n--- PRODUCTS ---");
                        System.out.println("1. View / search products");
                        System.out.println("2. Update product stock and details");
                        System.out.println("0. Back");
                        System.out.print("Choose an option: ");
                        String sub = sc.nextLine().trim();
                        switch (sub) {
                            case "1": System.out.println("View / search products coming soon..."); break;
                            case "2": System.out.println("Update product coming soon..."); break;
                            case "0": back = true; break;
                            default: System.out.println("Invalid option! Please choose 0-2.");
                        }
                    }
                    break;
                }

                case "2": {
                    boolean back = false;
                    while (!back) {
                        System.out.println("\n--- ORDERS ---");
                        System.out.println("1. View all orders");
                        System.out.println("2. Update order status");
                        System.out.println("3. Cancel order");
                        System.out.println("0. Back");
                        System.out.print("Choose an option: ");
                        String sub = sc.nextLine().trim();
                        switch (sub) {
                            case "1": System.out.println("View orders coming soon..."); break;
                            case "2": System.out.println("Update status (Pending > Processing > Shipped > Completed) coming soon..."); break;
                            case "3": System.out.println("Cancel order coming soon..."); break;
                            case "0": back = true; break;
                            default: System.out.println("Invalid option! Please choose 0-3.");
                        }
                    }
                    break;
                }

                case "3": {
                    boolean back = false;
                    while (!back) {
                        System.out.println("\n--- REPORTS ---");
                        System.out.println("1. Low-Stock Report");
                        System.out.println("0. Back");
                        System.out.print("Choose an option: ");
                        String sub = sc.nextLine().trim();
                        switch (sub) {
                            case "1": System.out.println("Low-stock report coming soon..."); break;
                            case "0": back = true; break;
                            default: System.out.println("Invalid option! Please choose 0-1.");
                        }
                    }
                    break;
                }

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
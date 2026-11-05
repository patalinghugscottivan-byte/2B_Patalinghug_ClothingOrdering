package com.scc.ui;

import com.scc.dao.OrderDao;
import com.scc.dao.ProductDao;
import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class StaffMenu {
    private final UserDao userDao;
    private final OrderDao orderDao = new OrderDao();
    private final ProductDao productDao = new ProductDao();
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
                        System.out.println("2. Update product stock");
                        System.out.println("0. Back");
                        System.out.print("Choose an option: ");
                        String sub = sc.nextLine().trim();
                        switch (sub) {
                            case "1": ProductView.browse(sc, productDao); break;
                            case "2": updateStock(staff); break;
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
                            case "2": updateOrderStatus(staff); break;
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
                            case "1": ProductView.print(productDao.getLowStock()); break;
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

    private void updateStock(User staff) {
        ProductView.print(productDao.getAllProducts());
        int id = InputUtil.readInt(sc, "Enter Product ID (0 to cancel): ", 0);
        if (id == 0) {
            System.out.println("-> Cancelled.");
            return;
        }
        if (productDao.getProductById(id) == null) {
            System.out.println("-> [ERROR] Product ID not found.");
            return;
        }
        int stock = InputUtil.readInt(sc, "New stock quantity: ", 0);
        System.out.println(productDao.updateStock(staff.user_id(), id, stock)
                ? "-> Stock updated." : "-> [ERROR] Failed to update stock.");
    }

    private void updateOrderStatus(User staff) {
        int orderId;
        while (true) {
            System.out.print("Enter Order ID (0 to cancel): ");
            try {
                orderId = Integer.parseInt(sc.nextLine().trim());
                break;
            } catch (NumberFormatException e) {
                System.out.println("-> Invalid input. Numbers only.");
            }
        }
        if (orderId == 0) {
            System.out.println("-> Cancelled.");
            return;
        }

        String newStatus;
        while (true) {
            System.out.print("New status [1] CONFIRMED | [2] PACKED | [3] COMPLETED | [0] Cancel: ");
            String s = sc.nextLine().trim();
            if (s.equals("1")) { newStatus = "CONFIRMED"; break; }
            if (s.equals("2")) { newStatus = "PACKED"; break; }
            if (s.equals("3")) { newStatus = "COMPLETED"; break; }
            if (s.equals("0")) { System.out.println("-> Cancelled."); return; }
            System.out.println("-> Invalid choice. Please enter 0-3.");
        }

        if (orderDao.updateOrderStatus(staff.user_id(), orderId, newStatus)) {
            System.out.println("-> Order #" + orderId + " is now " + newStatus + ".");
        } else {
            System.out.println("-> [ERROR] Order not found or update failed.");
        }
    }
}
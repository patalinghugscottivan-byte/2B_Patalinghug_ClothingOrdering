package com.scc.ui;

import com.scc.dao.OrderDao;
import com.scc.dao.ProductDao;
import com.scc.dao.UserDao;
import com.scc.model.Product;
import com.scc.model.User;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class CustomerMenu {
    private final UserDao userDao;
    private final OrderDao orderDao = new OrderDao();
    private final ProductDao productDao = new ProductDao();
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

                case "1":
                    ProductView.browse(sc, productDao);
                    break;

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
                            case "1": placeOrder(customer); break;
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

    private void placeOrder(User customer) {
        Map<Integer, Integer> cart = new LinkedHashMap<>();
        ProductView.print(productDao.searchProducts(null, null, null, null));

        while (true) {
            int id = InputUtil.readInt(sc, "Enter Product ID to add (0 to finish): ", 0);
            if (id == 0) break;

            Product p = productDao.getProductById(id);
            if (p == null || !"ACTIVE".equals(p.status())) {
                System.out.println("-> [ERROR] Product not available.");
                continue;
            }
            int qty = InputUtil.readInt(sc, "Quantity (0 to skip): ", 0);
            if (qty == 0) continue;

            if (cart.getOrDefault(id, 0) + qty > p.stockQuantity()) {
                System.out.println("-> [ERROR] Only " + p.stockQuantity() + " in stock.");
                continue;
            }
            cart.merge(id, qty, Integer::sum);
            System.out.println("-> Added " + qty + " x " + p.productName());
        }

        if (cart.isEmpty()) {
            System.out.println("-> Cart is empty. Order cancelled.");
            return;
        }

        double total = 0;
        System.out.println("\n--- ORDER SUMMARY ---");
        for (Map.Entry<Integer, Integer> e : cart.entrySet()) {
            Product p = productDao.getProductById(e.getKey());
            double sub = p.price() * e.getValue();
            total += sub;
            System.out.printf("%-24s x%-3d P%.2f%n", p.productName(), e.getValue(), sub);
        }
        System.out.printf("TOTAL: P%.2f (Cash on Delivery)%n", total);

        System.out.print("Confirm order? (Y/N): ");
        if (!sc.nextLine().trim().equalsIgnoreCase("Y")) {
            System.out.println("-> Order cancelled.");
            return;
        }

        int orderId = orderDao.placeOrder(customer.user_id(), customer.fullname(),
                cart, "Cash on Delivery", customer.address());

        if (orderId > 0) {
            System.out.println("-> Order #" + orderId + " placed! Status: PENDING.");
        } else {
            System.out.println("-> [ERROR] Failed to place order.");
        }
    }
}
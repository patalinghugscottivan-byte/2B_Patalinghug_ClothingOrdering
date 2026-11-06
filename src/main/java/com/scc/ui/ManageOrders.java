package com.scc.ui;

import com.scc.dao.OrderDao;
import com.scc.model.Order;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class ManageOrders {

    private static final String[] UPDATABLE = {"PROCESSING", "SHIPPED", "DELIVERED"};

    public static void run(Scanner sc, OrderDao orderDao, User admin) {
        while (true) {
            System.out.println("\n===============================");
            System.out.println("         MANAGE ORDERS");
            System.out.println("===============================");
            System.out.println("[1] View All Orders\n[2] Search Orders" +
                    "\n[3] Update Order Status\n[4] Cancel Order\n[0] Back");
            System.out.print("Enter Choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> viewAll(sc, orderDao);
                case "2" -> search(sc, orderDao);
                case "3" -> updateStatus(sc, orderDao, admin);
                case "4" -> cancel(sc, orderDao, admin);
                case "0" -> {
                    return;
                }
                default -> System.out.println("-> Invalid choice! 0 to 4 only.");
            }
        }
    }

    // ===== [1] View All =====
    private static void viewAll(Scanner sc, OrderDao orderDao) {
        List<Order> orders = orderDao.getAllOrders();
        printTable(orders, "ALL ORDERS");
        if (orders.isEmpty()) return;
        offerItemDetails(sc, orderDao);
    }

    // ===== [2] Search =====
    private static void search(Scanner sc, OrderDao orderDao) {
        System.out.println("\nSearch by: [1] Order ID | [2] Customer (User) ID | [3] Status | [0] Cancel");
        System.out.print("Enter Choice: ");
        String by = sc.nextLine().trim();

        switch (by) {
            case "1" -> {
                int id = InputUtil.readInt(sc, "Enter Order ID: ", 0);
                Order o = orderDao.getOrderById(id);
                printTable(o == null ? List.of() : List.of(o), "SEARCH RESULT");
                if (o != null) offerItemDetails(sc, orderDao);
            }
            case "2" -> {
                int uid = InputUtil.readInt(sc, "Enter Customer (User) ID: ", 0);
                List<Order> list = orderDao.getOrdersByUserId(uid);
                printTable(list, "ORDERS OF USER #" + uid);
                if (!list.isEmpty()) offerItemDetails(sc, orderDao);
            }
            case "3" -> {
                String status = pickStatus(sc, new String[]{"PENDING", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED"});
                if (status == null) return;
                List<Order> list = orderDao.getOrdersByStatus(status);
                printTable(list, status + " ORDERS");
                if (!list.isEmpty()) offerItemDetails(sc, orderDao);
            }
            case "0" -> System.out.println(" -> Operation Cancelled.");
            default -> System.out.println("-> Invalid choice.");
        }
    }

    // ===== [3] Update Status =====
    private static void updateStatus(Scanner sc, OrderDao orderDao, User admin) {
        printTable(orderDao.getAllOrders(), "UPDATE ORDER STATUS");

        int id = InputUtil.readInt(sc, "Enter Order ID to update (0 to cancel): ", 0);
        if (id == 0) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }

        Order o = orderDao.getOrderById(id);
        if (o == null) {
            System.out.println(" -> [ERROR] Order ID not found.");
            return;
        }
        if (o.orderStatus().equalsIgnoreCase("CANCELLED") || o.orderStatus().equalsIgnoreCase("DELIVERED")) {
            System.out.println(" -> [ERROR] Cannot update. Order is already " + o.orderStatus() + ".");
            return;
        }

        System.out.println("Current status: " + o.orderStatus());
        String newStatus = pickStatus(sc, UPDATABLE);
        if (newStatus == null) return;

        if (!confirm(sc, "Change Order #" + id + " to " + newStatus + "?")) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }

        if (orderDao.updateOrderStatus(admin.user_id(), id, newStatus)) {
            System.out.println(" -> Order #" + id + " is now " + newStatus + ".");
        } else {
            System.out.println(" -> [ERROR] Failed to update order status.");
        }
    }

    // ===== [4] Cancel =====
    private static void cancel(Scanner sc, OrderDao orderDao, User admin) {
        printTable(orderDao.getAllOrders(), "CANCEL ORDER");

        int id = InputUtil.readInt(sc, "Enter Order ID to cancel (0 to cancel): ", 0);
        if (id == 0) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }

        Order o = orderDao.getOrderById(id);
        if (o == null) {
            System.out.println(" -> [ERROR] Order ID not found.");
            return;
        }
        if (o.orderStatus().equalsIgnoreCase("CANCELLED")) {
            System.out.println(" -> [ERROR] Order is already cancelled.");
            return;
        }
        if (!(o.orderStatus().equalsIgnoreCase("PENDING") || o.orderStatus().equalsIgnoreCase("PROCESSING"))) {
            System.out.println(" -> [ERROR] Cannot cancel. Order is already " + o.orderStatus() + ".");
            return;
        }

        if (!confirm(sc, "Cancel Order #" + id + "? Stock will be restored.")) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }

        if (orderDao.cancelOrder(admin.user_id(), id)) {
            System.out.println(" -> Order #" + id + " has been cancelled and stock restored.");
        } else {
            System.out.println(" -> [ERROR] Failed to cancel order.");
        }
    }

    // ===== Helpers =====
    static void printTable(List<Order> orders, String title) {
        System.out.println("\n====================== " + title + " ======================");
        if (orders.isEmpty()) {
            System.out.println("\n-> No Records.");
            return;
        }

        String border = "+------+---------+--------------+-----------------+------------+------------------------------+";
        String format = "| %-4s | %-7s | %-12s | %-15s | %-10s | %-28s |\n";

        System.out.println();
        System.out.println(border);
        System.out.printf(format, "ID", "User ID", "Total (P)", "Payment", "Status", "Delivery Address");
        System.out.println(border);
        for (Order o : orders) {
            System.out.printf(format, o.orderId(), o.userId(),
                    String.format("%.2f", o.totalAmount()),
                    cut(o.paymentMethod(), 15), cut(o.orderStatus(), 10), cut(o.deliveryAddress(), 28));
        }
        System.out.println(border);
    }

    private static void offerItemDetails(Scanner sc, OrderDao orderDao) {
        int id = InputUtil.readInt(sc, "Enter Order ID to see items (0 to skip): ", 0);
        if (id == 0) return;

        List<OrderDao.Item> items = orderDao.getOrderItems(id);
        if (items.isEmpty()) {
            System.out.println(" -> No items found for Order #" + id + ".");
            return;
        }

        String border = "+------------+-----+--------------+--------------+";
        String format = "| %-10s | %-3s | %-12s | %-12s |\n";
        System.out.println("\n--- ITEMS OF ORDER #" + id + " ---");
        System.out.println(border);
        System.out.printf(format, "Product ID", "Qty", "Unit Price", "Subtotal");
        System.out.println(border);
        for (OrderDao.Item i : items) {
            System.out.printf(format, i.productId(), i.quantity(),
                    String.format("%.2f", i.unitPrice()), String.format("%.2f", i.subtotal()));
        }
        System.out.println(border);
    }

    private static String pickStatus(Scanner sc, String[] options) {
        System.out.println("\nSelect status:");
        for (int i = 0; i < options.length; i++) {
            System.out.println("[" + (i + 1) + "] " + options[i]);
        }
        System.out.println("[0] Cancel");
        int s = InputUtil.readInt(sc, "Enter Choice: ", 0);
        if (s == 0) {
            System.out.println(" -> Operation Cancelled.");
            return null;
        }
        if (s < 1 || s > options.length) {
            System.out.println(" -> [ERROR] Invalid choice.");
            return null;
        }
        return options[s - 1];
    }

    private static boolean confirm(Scanner sc, String message) {
        System.out.print(message + " (Y/N): ");
        return sc.nextLine().trim().equalsIgnoreCase("Y");
    }

    private static String cut(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 3) + "..." : s;
    }
}
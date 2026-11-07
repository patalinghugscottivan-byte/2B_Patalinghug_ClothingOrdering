package com.scc.ui;

import com.scc.dao.OrderDao;
import com.scc.dao.ProductDao;
import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class StaffMenu {

    private final UserDao userDao;
    private final ProductDao productDao = new ProductDao();
    private final OrderDao orderDao = new OrderDao();
    private final Scanner sc;

    public StaffMenu(UserDao userDao, Scanner sc) {
        this.userDao = userDao;
        this.sc = sc;
    }

    public void display(User staff) {
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("\n==============================");
            System.out.println("           STAFF MENU ");
            System.out.println(" User: " + staff.username());
            System.out.println("==============================");
            System.out.println("1. View / Search Products");
            System.out.println("2. Manage Orders");
            System.out.println("3. Low-Stock Report");
            System.out.println("4. Edit My Profile");
            System.out.println("0. Logout");
            System.out.print("Enter choice: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> ProductView.browse(sc, productDao);
                case "2" -> ManageOrders.run(sc, orderDao, staff);
                case "3" -> ProductView.print(productDao.getLowStock());
                case "4" -> ProfileView.edit(sc, staff);
                case "0" -> {
                    if (confirm("Are you sure you want to logout?")) {
                        System.out.println("You have been logged out. Goodbye, " + staff.username() + "!");
                        loggedIn = false;
                    }
                }
                default -> System.out.println("-> Invalid choice. Try again.");
            }
        }
    }

    private boolean confirm(String message) {
        System.out.print(message + " (Y/N): ");
        return sc.nextLine().trim().equalsIgnoreCase("Y");
    }
}
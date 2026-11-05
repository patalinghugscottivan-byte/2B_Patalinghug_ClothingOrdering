package com.scc.ui;

import com.scc.dao.ProductDao;
import com.scc.model.Product;

import java.util.List;
import java.util.Scanner;

public final class ProductView {
    private ProductView() {}

    private static final String FMT =
            "| %-4s | %-24s | %-10s | %-4s | %-8s | %10s | %5s | %-12s |%n";
    private static final String BORDER = "+" + "-".repeat(6) + "+" + "-".repeat(26) + "+"
            + "-".repeat(12) + "+" + "-".repeat(6) + "+" + "-".repeat(10) + "+"
            + "-".repeat(12) + "+" + "-".repeat(7) + "+" + "-".repeat(14) + "+";

    public static void print(List<Product> products) {
        if (products.isEmpty()) {
            System.out.println("\n-> No products found.");
            return;
        }
        System.out.println();
        System.out.println(BORDER);
        System.out.printf(FMT, "ID", "Name", "Category", "Size", "Color", "Price", "Stock", "Status");
        System.out.println(BORDER);
        for (Product p : products) {
            String name = p.productName().length() > 24
                    ? p.productName().substring(0, 21) + "..." : p.productName();
            System.out.printf(FMT, p.productId(), name, p.category(), p.size(), p.color(),
                    String.format("P%.2f", p.price()), p.stockQuantity(), p.status());
        }
        System.out.println(BORDER);
    }

    public static void browse(Scanner sc, ProductDao dao) {
        while (true) {
            System.out.println("\n--- BROWSE PRODUCTS ---");
            System.out.println("[1] View All Active\n[2] Search by Name\n[3] Filter by Category"
                    + "\n[4] Filter by Price Range\n[0] Back");
            System.out.print("Enter Choice: ");
            String c = sc.nextLine().trim();
            switch (c) {
                case "1" -> print(dao.searchProducts(null, null, null, null));
                case "2" -> {
                    String kw = InputUtil.readRequired(sc, "Keyword: ");
                    print(dao.searchProducts(kw, null, null, null));
                }
                case "3" -> {
                    System.out.println("Categories: " + String.join(", ", dao.getCategories()));
                    String cat = InputUtil.readRequired(sc, "Category: ");
                    print(dao.searchProducts(null, cat, null, null));
                }
                case "4" -> {
                    double min = InputUtil.readDouble(sc, "Min price: ", 0);
                    double max = InputUtil.readDouble(sc, "Max price: ", min);
                    print(dao.searchProducts(null, null, min, max));
                }
                case "0" -> { return; }
                default -> System.out.println("-> Invalid choice! 0 to 4 only.");
            }
        }
    }
}
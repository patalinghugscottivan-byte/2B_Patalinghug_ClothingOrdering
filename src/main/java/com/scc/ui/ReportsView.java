package com.scc.ui;

import com.scc.dao.ReportDao;
import com.scc.model.Order;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class ReportsView {

    public static void salesReport(Scanner sc) {
        ReportDao dao = new ReportDao();

        if (dao.dateColumn() == null) {
            System.out.println("\n-> [ERROR] No date column found in the orders table, so the sales report cannot be generated.");
            return;
        }

        while (true) {
            System.out.println("\n===============================");
            System.out.println("         SALES REPORT");
            System.out.println("===============================");
            System.out.println("[1] Daily Sales Report\n[2] Monthly Sales Report\n[0] Back");
            System.out.print("Enter Choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> daily(sc, dao);
                case "2" -> monthly(sc, dao);
                case "0" -> {
                    return;
                }
                default -> System.out.println("-> Invalid choice! 0 to 2 only.");
            }
        }
    }

    private static void daily(Scanner sc, ReportDao dao) {
        System.out.print("Enter date (YYYY-MM-DD) or press Enter for today: ");
        String input = sc.nextLine().trim();

        LocalDate date;
        try {
            date = input.isEmpty() ? LocalDate.now() : LocalDate.parse(input);
        } catch (DateTimeParseException e) {
            System.out.println(" -> [ERROR] Invalid date. Example: 2026-11-08");
            return;
        }

        String day = date.toString();
        ReportDao.Summary s = dao.getSummary(day);

        System.out.println("\n====== DAILY SALES REPORT: " + day + " ======");
        printSummary(s);

        List<Order> orders = dao.getOrdersOn(day);
        if (!orders.isEmpty()) {
            ManageOrders.printTable(orders, "ORDERS ON " + day);
        }
    }

    private static void monthly(Scanner sc, ReportDao dao) {
        System.out.print("Enter month (YYYY-MM) or press Enter for this month: ");
        String input = sc.nextLine().trim();

        YearMonth ym;
        try {
            ym = input.isEmpty() ? YearMonth.now() : YearMonth.parse(input);
        } catch (DateTimeParseException e) {
            System.out.println(" -> [ERROR] Invalid month. Example: 2026-11");
            return;
        }

        String month = ym.toString();
        ReportDao.Summary s = dao.getSummary(month);

        System.out.println("\n====== MONTHLY SALES REPORT: " + month + " ======");
        printSummary(s);

        List<ReportDao.DayRow> rows = dao.getDailyBreakdown(month);
        if (rows.isEmpty()) return;

        String border = "+------------+--------+----------------+";
        String format = "| %-10s | %-6s | %-14s |\n";
        System.out.println("\n--- DAILY BREAKDOWN ---");
        System.out.println(border);
        System.out.printf(format, "Date", "Orders", "Sales (P)");
        System.out.println(border);
        for (ReportDao.DayRow r : rows) {
            System.out.printf(format, r.day(), r.orders(), String.format("%.2f", r.sales()));
        }
        System.out.println(border);
    }

    private static void printSummary(ReportDao.Summary s) {
        System.out.println("Total orders     : " + s.totalOrders());
        System.out.println("Cancelled orders : " + s.cancelledOrders());
        System.out.println("Valid orders     : " + (s.totalOrders() - s.cancelledOrders()));
        System.out.printf("Net sales        : P%.2f (excluding cancelled orders)%n", s.netSales());
    }
}
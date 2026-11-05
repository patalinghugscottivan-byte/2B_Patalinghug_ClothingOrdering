package com.scc.ui;

import java.util.Scanner;

public final class InputUtil {
    private InputUtil() {}

    public static int readInt(Scanner sc, String prompt, int min) {
        while (true) {
            System.out.print(prompt);
            try {
                int v = Integer.parseInt(sc.nextLine().trim());
                if (v >= min) return v;
                System.out.println(" -> [ERROR] Value must be at least " + min + ".");
            } catch (NumberFormatException e) {
                System.out.println(" -> [ERROR] Please enter a valid whole number.");
            }
        }
    }

    public static double readDouble(Scanner sc, String prompt, double min) {
        while (true) {
            System.out.print(prompt);
            try {
                double v = Double.parseDouble(sc.nextLine().trim());
                if (v >= min) return v;
                System.out.println(" -> [ERROR] Value must be at least " + min + ".");
            } catch (NumberFormatException e) {
                System.out.println(" -> [ERROR] Please enter a valid number.");
            }
        }
    }

    public static String readRequired(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            if (!s.isEmpty()) return s;
            System.out.println(" -> [ERROR] This field cannot be empty.");
        }
    }
}
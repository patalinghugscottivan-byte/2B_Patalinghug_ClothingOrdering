package com.scc.ui;

import com.scc.dao.ProfileDao;
import com.scc.model.User;

import java.util.Scanner;

public class ProfileView {

    public static void edit(Scanner sc, User user) {
        ProfileDao dao = new ProfileDao();
        ProfileDao.Profile cur = dao.getProfile(user.user_id());

        if (cur == null) {
            System.out.println("\n-> [ERROR] Could not load your profile.");
            return;
        }

        System.out.println("\n===============================");
        System.out.println("         EDIT MY PROFILE");
        System.out.println("===============================");
        System.out.println("Username : " + user.username() + " (cannot be changed)");
        System.out.println("Role     : " + user.role());
        System.out.println("(Press Enter to keep the current value of a field)\n");

        String fullName = ask(sc, "Full name", cur.fullName(), null,
                "Full name is required.");
        String email = ask(sc, "Email", cur.email(), "^[\\w.+-]+@[\\w-]+\\.[\\w.]+$",
                "Invalid email format (example: name@gmail.com).");
        String contact = ask(sc, "Contact number (11 digits)", cur.contact(), "\\d{11}",
                "Must be exactly 11 digits.");
        String address = ask(sc, "Address", cur.address(), null,
                "Address is required.");

        if (fullName.equals(cur.fullName()) && email.equals(cur.email())
                && contact.equals(cur.contact()) && address.equals(cur.address())) {
            System.out.println("\n-> No changes made.");
            return;
        }

        System.out.print("\nSave changes? (Y/N): ");
        if (!sc.nextLine().trim().equalsIgnoreCase("Y")) {
            System.out.println("-> Operation Cancelled.");
            return;
        }

        boolean ok = dao.updateProfile(user.user_id(),
                new ProfileDao.Profile(fullName, email, contact, address));

        if (ok) {
            System.out.println("-> Profile updated. The new info will show on your next login.");
        } else {
            System.out.println("-> [ERROR] Failed to update profile.");
        }
    }

    /** Enter = keep the current value. If a regex is given, validate the new value. */
    private static String ask(Scanner sc, String label, String current, String regex, String errorMsg) {
        while (true) {
            String shown = (current == null || current.isBlank()) ? "-" : current;
            System.out.print(label + " [" + shown + "]: ");
            String input = sc.nextLine().trim();

            if (input.isEmpty()) {
                if (current == null || current.isBlank()) {
                    System.out.println(" -> [ERROR] " + errorMsg);
                    continue;
                }
                return current;
            }
            if (regex != null && !input.matches(regex)) {
                System.out.println(" -> [ERROR] " + errorMsg);
                continue;
            }
            return input;
        }
    }
}
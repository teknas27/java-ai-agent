package com.example.bad;

import java.util.*;
import java.sql.*;

public class OrderProcessor {

    public static Connection conn;
    public static int MAX = 100;
    public static String DB = "jdbc:mysql://localhost:3306/shop";
    private ArrayList orders = new ArrayList();
    private double tax = 0.18;
    private int count = 0;

    public String processOrder(String customerId, String item, int qty, double price,
                               String address, String city, String state, String zip,
                               String phone, String email, boolean isPremium) {

        double total = 0;
        double discount = 0;
        double shipping = 0;
        String status = "";
        String msg = "";

        if (qty > 0) {
            total = qty * price;
            if (isPremium) {
                discount = total * 0.15;
                total = total - discount;
                if (total > 500) {
                    shipping = 0;
                } else {
                    shipping = 25;
                }
            } else {
                if (total > 1000) {
                    discount = total * 0.05;
                    total = total - discount;
                    shipping = 50;
                } else if (total > 500) {
                    discount = total * 0.02;
                    total = total - discount;
                    shipping = 75;
                } else {
                    shipping = 100;
                }
            }

            total = total + shipping;
            total = total + (total * tax);

            if (total > 10000) {
                status = "NEEDS_APPROVAL";
                msg = "Order for " + customerId + " needs manager approval. Total: $" + total;
                System.out.println(msg);
            } else {
                status = "APPROVED";
                msg = "Order approved for " + customerId + ". Total: $" + total;
                System.out.println(msg);
            }

            try {
                conn = DriverManager.getConnection(DB, "root", "password123");
                Statement stmt = conn.createStatement();
                stmt.executeUpdate("INSERT INTO orders VALUES('" + customerId + "','" + item + "'," + qty + "," + total + ",'" + status + "')");
                stmt.executeUpdate("UPDATE inventory SET stock = stock - " + qty + " WHERE item = '" + item + "'");
                stmt.executeUpdate("INSERT INTO audit_log VALUES('" + customerId + "','" + new Date() + "','" + msg + "')");
            } catch (Exception e) {
                e.printStackTrace();
            }

            count++;
            orders.add(item);

        } else {
            msg = "Invalid quantity";
            System.out.println(msg);
        }

        return msg;
    }

    public void printReport() {
        for (int i = 0; i < orders.size(); i++) {
            System.out.println("Order " + i + ": " + orders.get(i));
        }
        System.out.println("Total orders: " + count);
    }
}

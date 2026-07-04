package com.example.bad;

import java.util.*;

public class NotificationService {

    public void sendNotification(String type, String recipient, String message,
                                  String subject, Map<String, String> metadata) {

        if (type.equals("EMAIL")) {
            System.out.println("Connecting to SMTP server...");
            System.out.println("From: noreply@app.com");
            System.out.println("To: " + recipient);
            System.out.println("Subject: " + subject);
            System.out.println("Body: " + message);
            System.out.println("Email sent!");

        } else if (type.equals("SMS")) {
            System.out.println("Connecting to SMS gateway...");
            String truncated = message.length() > 160 ? message.substring(0, 160) : message;
            System.out.println("To: " + recipient);
            System.out.println("Message: " + truncated);
            System.out.println("SMS sent!");

        } else if (type.equals("PUSH")) {
            System.out.println("Connecting to push service...");
            System.out.println("Device: " + recipient);
            System.out.println("Title: " + subject);
            System.out.println("Body: " + message);
            if (metadata != null && metadata.containsKey("icon")) {
                System.out.println("Icon: " + metadata.get("icon"));
            }
            System.out.println("Push notification sent!");

        } else if (type.equals("SLACK")) {
            System.out.println("Connecting to Slack API...");
            System.out.println("Channel: " + recipient);
            System.out.println("Message: " + message);
            if (metadata != null && metadata.containsKey("thread_ts")) {
                System.out.println("Thread: " + metadata.get("thread_ts"));
            }
            System.out.println("Slack message sent!");

        } else if (type.equals("WEBHOOK")) {
            System.out.println("Posting to webhook URL: " + recipient);
            System.out.println("Payload: " + message);
            System.out.println("Webhook delivered!");
        }
    }

    // God class: handles pricing, discounting, and formatting
    public double calculatePrice(String productType, double basePrice, int quantity,
                                  String customerTier, String couponCode) {

        double price = basePrice * quantity;

        // Discount logic embedded directly
        if (customerTier.equals("GOLD")) {
            price = price * 0.85;
        } else if (customerTier.equals("SILVER")) {
            price = price * 0.90;
        } else if (customerTier.equals("BRONZE")) {
            price = price * 0.95;
        }

        // Coupon logic - should be its own class
        if (couponCode != null) {
            if (couponCode.equals("SAVE10")) {
                price = price - 10;
            } else if (couponCode.equals("SAVE20")) {
                price = price - 20;
            } else if (couponCode.equals("HALF")) {
                price = price * 0.5;
            }
        }

        // Tax logic - varies by product type
        if (productType.equals("ELECTRONICS")) {
            price = price * 1.18;
        } else if (productType.equals("FOOD")) {
            price = price * 1.05;
        } else if (productType.equals("CLOTHING")) {
            price = price * 1.12;
        } else {
            price = price * 1.15;
        }

        return price;
    }

    // Observer pattern needed: tightly coupled event handling
    public void onUserAction(String action, String userId, Map<String, Object> data) {
        // Analytics
        System.out.println("[ANALYTICS] User " + userId + " did " + action);

        // Logging
        System.out.println("[LOG] " + new Date() + " - " + userId + " - " + action);

        // Cache invalidation
        if (action.equals("PROFILE_UPDATE")) {
            System.out.println("[CACHE] Invalidating user cache for " + userId);
        }

        // Notification trigger
        if (action.equals("PURCHASE")) {
            sendNotification("EMAIL", userId, "Thanks for your purchase!", "Order Confirmation", null);
        }

        // Points calculation
        if (action.equals("PURCHASE")) {
            double amount = (double) data.get("amount");
            int points = (int) (amount * 10);
            System.out.println("[POINTS] Adding " + points + " points to " + userId);
        }
    }
}

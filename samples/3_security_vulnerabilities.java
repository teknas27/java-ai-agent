package com.example.bad;

import java.sql.*;
import java.security.MessageDigest;
import java.util.Random;
import javax.xml.parsers.*;
import org.xml.sax.InputSource;
import java.io.*;

public class UserAuthController {

    private static final String DB_URL = "jdbc:mysql://prod-db.internal:3306/users";
    private static final String DB_USER = "admin";
    private static final String DB_PASS = "SuperSecret@2024!";
    private static final String API_KEY = "ak_live_7f8g9h0j1k2l3m4n5o6p";

    public boolean login(String username, String password) {
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

            // SQL Injection vulnerability
            String query = "SELECT * FROM users WHERE username = '" + username
                         + "' AND password = '" + password + "'";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            if (rs.next()) {
                System.out.println("Login successful for: " + username + " with password: " + password);
                return true;
            }
        } catch (Exception e) {
            // Swallowed exception - no proper error handling
        }
        return false;
    }

    public String hashPassword(String password) {
        try {
            // Weak hashing algorithm
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return password; // Returns plain text on failure!
        }
    }

    public String generateToken() {
        // Insecure random - predictable tokens
        Random random = new Random();
        StringBuilder token = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            token.append((char) (random.nextInt(26) + 'a'));
        }
        return token.toString();
    }

    public String processUserXml(String xmlInput) {
        try {
            // XXE vulnerability - external entities not disabled
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xmlInput));
            org.w3c.dom.Document doc = builder.parse(is);
            return doc.getDocumentElement().getTextContent();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public void updateUserRole(String userId, String newRole) {
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            // No authorization check - any user can escalate privileges
            String query = "UPDATE users SET role = '" + newRole + "' WHERE id = '" + userId + "'";
            conn.createStatement().executeUpdate(query);
            System.out.println("Role updated to " + newRole + " for user " + userId);
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public void logSensitiveData(String userId, String creditCard, String ssn) {
        // Logging sensitive data in plain text
        System.out.println("Processing payment for user: " + userId);
        System.out.println("Credit Card: " + creditCard);
        System.out.println("SSN: " + ssn);
        System.out.println("API Key used: " + API_KEY);
    }
}

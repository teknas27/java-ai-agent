package com.example;

import java.sql.*;
import java.security.MessageDigest;
import java.util.Random;

public class AuthService {

    public boolean login(String user, String pass) {
        try {
            Connection c = DriverManager.getConnection("jdbc:mysql://localhost/db", "root", "admin123");
            ResultSet rs = c.createStatement().executeQuery(
                "SELECT * FROM users WHERE name='" + user + "' AND pass='" + pass + "'");
            return rs.next();
        } catch (Exception e) { }
        return false;
    }

    public String hash(String password) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] b = md.digest(password.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    public String generateToken() {
        Random r = new Random();
        StringBuilder t = new StringBuilder();
        for (int i = 0; i < 16; i++) t.append((char)(r.nextInt(26) + 'a'));
        return t.toString();
    }
}

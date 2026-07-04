package com.example.bad;

import java.util.*;
import java.util.stream.*;

public class DataProcessor {

    private List<Map<String, Object>> cache = new ArrayList<>();
    private static DataProcessor instance;

    // Singleton anti-pattern with lazy init (not thread-safe)
    public static DataProcessor getInstance() {
        if (instance == null) {
            instance = new DataProcessor();
        }
        return instance;
    }

    // N+1 query simulation: fetches related data in a loop
    public List<Map<String, Object>> getUserOrders(List<String> userIds) {
        List<Map<String, Object>> results = new ArrayList<>();

        for (String userId : userIds) {
            // Simulates N+1: one query per user instead of batch
            Map<String, Object> user = fetchUserFromDb(userId);
            List<Map<String, Object>> orders = fetchOrdersForUser(userId);
            List<Map<String, Object>> payments = fetchPaymentsForUser(userId);
            List<Map<String, Object>> addresses = fetchAddressesForUser(userId);

            user.put("orders", orders);
            user.put("payments", payments);
            user.put("addresses", addresses);
            results.add(user);
        }
        return results;
    }

    // Unbounded cache - memory leak
    public Map<String, Object> getCachedData(String key) {
        for (Map<String, Object> item : cache) {
            if (item.get("key").equals(key)) {
                return item;
            }
        }
        Map<String, Object> data = fetchFromExternalService(key);
        cache.add(data); // Never evicts - grows forever
        return data;
    }

    // Inefficient string concatenation in loop
    public String generateReport(List<Map<String, Object>> records) {
        String report = "";
        for (int i = 0; i < records.size(); i++) {
            Map<String, Object> record = records.get(i);
            report = report + "Record #" + i + ": ";
            report = report + "Name=" + record.get("name") + ", ";
            report = report + "Value=" + record.get("value") + ", ";
            report = report + "Date=" + record.get("date") + "\n";
        }
        return report;
    }

    // Synchronizing on entire method - unnecessary contention
    public synchronized List<String> processItems(List<String> items) {
        List<String> processed = new ArrayList<>();
        for (String item : items) {
            // Expensive operation inside sync block
            String result = expensiveTransformation(item);
            processed.add(result);
        }
        return processed;
    }

    // Nested loops - O(n^3) complexity
    public List<int[]> findTriplets(int[] arr, int targetSum) {
        List<int[]> triplets = new ArrayList<>();
        for (int i = 0; i < arr.length; i++) {
            for (int j = i + 1; j < arr.length; j++) {
                for (int k = j + 1; k < arr.length; k++) {
                    if (arr[i] + arr[j] + arr[k] == targetSum) {
                        triplets.add(new int[]{arr[i], arr[j], arr[k]});
                    }
                }
            }
        }
        return triplets;
    }

    // Creating objects in hot loop
    public List<Double> calculateMetrics(List<Map<String, Object>> dataPoints) {
        List<Double> metrics = new ArrayList<>();
        for (Map<String, Object> point : dataPoints) {
            // Creates new formatter every iteration
            java.text.DecimalFormat df = new java.text.DecimalFormat("#.##");
            double value = (double) point.get("value");
            double weight = (double) point.get("weight");
            double result = Double.parseDouble(df.format(value * weight / 100.0));
            metrics.add(result);
        }
        return metrics;
    }

    // Boxing/unboxing overhead in tight loop
    public Long sumValues(List<Long> values) {
        Long sum = 0L;
        for (int i = 0; i < values.size(); i++) {
            sum = sum + values.get(i); // autoboxing on every iteration
        }
        return sum;
    }

    private Map<String, Object> fetchUserFromDb(String id) {
        return new HashMap<>(Map.of("id", id, "key", id));
    }

    private List<Map<String, Object>> fetchOrdersForUser(String id) {
        return new ArrayList<>();
    }

    private List<Map<String, Object>> fetchPaymentsForUser(String id) {
        return new ArrayList<>();
    }

    private List<Map<String, Object>> fetchAddressesForUser(String id) {
        return new ArrayList<>();
    }

    private Map<String, Object> fetchFromExternalService(String key) {
        return new HashMap<>(Map.of("key", key, "data", "value"));
    }

    private String expensiveTransformation(String item) {
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        return item.toUpperCase();
    }
}

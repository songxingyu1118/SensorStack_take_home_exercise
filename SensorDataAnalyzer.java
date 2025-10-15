import java.io.*;
import java.util.*;

/*
 * IoT Sensor Data Analyzer
 *
 * Reads a large CSV file of sensor data,
 * calculates average, min, max, count, stdDev
 * for each combination of (site + device + metric),
 * and prints the top 10 groups by average and variability.
 */

public class SensorDataAnalyzer {

    // Represents one (site, device, metric) combination
    static class Key {
        String site, device, metric;

        Key(String site, String device, String metric) {
            this.site = site;
            this.device = device;
            this.metric = metric;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Key)) return false;
            Key k = (Key) o;
            return site.equals(k.site) && device.equals(k.device) && metric.equals(k.metric);
        }

        @Override
        public int hashCode() {
            return Objects.hash(site, device, metric);
        }

        @Override
        public String toString() {
            return site + " | " + device + " | " + metric;
        }
    }

    /** Stores running stats using a stable method (Welford's algorithm) */
    static class Stats {
        long count = 0;
        double mean = 0;
        double m2 = 0;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;

        void add(double x) {
            count++;
            if (x < min) min = x;
            if (x > max) max = x;

            // Welford’s online algorithm
            double delta = x - mean;
            mean += delta / count;
            m2 += delta * (x - mean);
        }

        double getAverage() { return mean; }
        double getStdDev() { return count > 1 ? Math.sqrt(m2 / (count - 1)) : 0; }

        @Override
        public String toString() {
            return String.format("count=%d avg=%.2f min=%.2f max=%.2f stddev=%.2f",
                    count, mean, min, max, getStdDev());
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java SensorDataAnalyzer <file.csv>");
            return;
        }

        String fileName = args[0];
        Map<Key, Stats> data = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String header = reader.readLine(); // skip header
            if (header == null) {
                System.out.println("Empty file!");
                return;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] cols = line.split(",");

                // skip bad lines
                if (cols.length < 6) continue;

                String site = cols[1].trim();
                String device = cols[2].trim();
                String metric = cols[3].trim();
                String valueStr = cols[5].trim();

                double value;
                try {
                    value = Double.parseDouble(valueStr);
                } catch (NumberFormatException e) {
                    continue; // skip non-number
                }

                Key key = new Key(site, device, metric);
                Stats stats = data.getOrDefault(key, new Stats());
                stats.add(value);
                data.put(key, stats);
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            return;
        }

        // Sort and print top 10 by average
        System.out.println("\nTop 10 by Highest Average:");
        data.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue().getAverage(), a.getValue().getAverage()))
                .limit(10)
                .forEach(e -> System.out.println(e.getKey() + " -> " + e.getValue()));

        // Sort and print top 10 by stddev
        System.out.println("\nTop 10 by Highest Variability (Std Dev):");
        data.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue().getStdDev(), a.getValue().getStdDev()))
                .limit(10)
                .forEach(e -> System.out.println(e.getKey() + " -> " + e.getValue()));
    }
}

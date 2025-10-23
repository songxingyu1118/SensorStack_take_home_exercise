import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import java.util.List;

class SensorStats {
    String site;
    String device;
    String metric;
    double sum = 0;
    double min = Double.MAX_VALUE;
    double max = Double.MIN_VALUE;
    double sumSq = 0;
    int count = 0;

    SensorStats(String site, String device, String metric) {
        this.site = site;
        this.device = device;
        this.metric = metric;
    }

    void addValue(double value) {
        sum += value;
        sumSq += value * value;
        if (value < min) min = value;
        if (value > max) max = value;
        count++;
    }

    double getAverage() {
        return sum / count;
    }

    double getStdDev() {
        double mean = getAverage();
        return Math.sqrt((sumSq / count) - (mean * mean));
    }

    String getKey() {
        return site + "-" + device + "-" + metric;
    }
}

class SimpleChart {
    public static void saveBarChart(List<SensorStats> statsList, String fileName) {
        int width = 800;
        int height = 400;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        // white background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        g.setColor(Color.BLUE);
        g.setFont(new Font("Arial", Font.PLAIN, 12));

        int topN = Math.min(10, statsList.size());
        double maxAvg = statsList.get(0).getAverage();  // top average value
        int barWidth = width / (topN * 2);
        int x = barWidth;

        for (int i = 0; i < topN; i++) {
            SensorStats s = statsList.get(i);
            int barHeight = (int) ((s.getAverage() / maxAvg) * (height - 100));
            int y = height - barHeight - 40;

            // draw bar
            g.fillRect(x, y, barWidth, barHeight);
            g.drawString(String.format("%.1f", s.getAverage()), x, y - 5);
            g.drawString(s.device, x, height - 20);

            x += barWidth * 2;
        }

        g.dispose();

        try {
            ImageIO.write(image, "png", new File(fileName));
            System.out.println("Chart saved as " + fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


public class SensorDataAnalyzer {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java SensorDataAnalyzer <csv-file>");
            return;
        }

        String fileName = args[0];
        List<SensorStats> statsList = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line = br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 6) continue;

                String time = parts[0];
                String site = parts[1];
                String device = parts[2];
                String metric = parts[3];
                String unit = parts[4];
                double value = Double.parseDouble(parts[5]);

                // find if this combination exists
                SensorStats found = null;
                for (SensorStats s : statsList) {
                    if (s.site.equals(site) && s.device.equals(device) && s.metric.equals(metric)) {
                        found = s;
                        break;
                    }
                }

                if (found == null) {
                    found = new SensorStats(site, device, metric);
                    statsList.add(found);
                }

                found.addValue(value);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        // sort by average
        statsList.sort((a, b) -> Double.compare(b.getAverage(), a.getAverage()));
        System.out.println("Top 10 by Highest Average:");
        for (int i = 0; i < Math.min(10, statsList.size()); i++) {
            SensorStats s = statsList.get(i);
            System.out.printf("%s | avg=%.2f | min=%.2f | max=%.2f | count=%d | std=%.2f%n",
                    s.getKey(), s.getAverage(), s.min, s.max, s.count, s.getStdDev());
        }

        // sort by std deviation
        statsList.sort((a, b) -> Double.compare(b.getStdDev(), a.getStdDev()));
        System.out.println("\nTop 10 by Highest Std Dev:");
        for (int i = 0; i < Math.min(10, statsList.size()); i++) {
            SensorStats s = statsList.get(i);
            System.out.printf("%s | avg=%.2f | min=%.2f | max=%.2f | count=%d | std=%.2f%n",
                    s.getKey(), s.getAverage(), s.min, s.max, s.count, s.getStdDev());
        }

        SimpleChart.saveBarChart(statsList, "chart.png");

    }
}

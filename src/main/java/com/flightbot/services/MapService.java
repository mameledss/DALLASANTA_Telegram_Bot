package com.flightbot.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class MapService {
    private static final Logger logger = LoggerFactory.getLogger(MapService.class);
    private static final String TILES_URL = "https://tile.openstreetmap.org";

    public File generateFlightMap(double fromLat, double fromLon, double toLat, double toLon, String flightNumber) {
        try {
            int width = 800;
            int height = 600;
            BufferedImage map = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = map.createGraphics();

            // Anti-aliasing
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Background
            g.setColor(new Color(200, 220, 255));
            g.fillRect(0, 0, width, height);

            // Calculate bounds
            double minLat = Math.min(fromLat, toLat) - 2;
            double maxLat = Math.max(fromLat, toLat) + 2;
            double minLon = Math.min(fromLon, toLon) - 2;
            double maxLon = Math.max(fromLon, toLon) + 2;

            // Convert coordinates to pixel positions
            int x1 = lonToX(fromLon, minLon, maxLon, width);
            int y1 = latToY(fromLat, minLat, maxLat, height);
            int x2 = lonToX(toLon, minLon, maxLon, width);
            int y2 = latToY(toLat, minLat, maxLat, height);

            // Draw route line
            g.setColor(new Color(255, 100, 100));
            g.setStroke(new BasicStroke(3));
            g.drawLine(x1, y1, x2, y2);

            // Draw airports
            g.setColor(new Color(0, 100, 200));
            g.fillOval(x1 - 8, y1 - 8, 16, 16);
            g.fillOval(x2 - 8, y2 - 8, 16, 16);

            // Draw labels
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 14));
            g.drawString("Partenza", x1 + 10, y1 - 10);
            g.drawString("Arrivo", x2 + 10, y2 - 10);

            // Title
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.drawString("Volo " + flightNumber, 10, 30);

            g.dispose();

            // Save to temporary file
            File tempFile = File.createTempFile("flight_map_", ".png");
            ImageIO.write(map, "PNG", tempFile);
            tempFile.deleteOnExit();

            return tempFile;
        } catch (IOException e) {
            logger.error("Error generating flight map", e);
            return null;
        }
    }

    public File generateLiveTrackingMap(double lat, double lon, String flightNumber, int altitude, int speed) {
        try {
            int width = 800;
            int height = 600;
            BufferedImage map = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = map.createGraphics();

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Background
            g.setColor(new Color(200, 220, 255));
            g.fillRect(0, 0, width, height);

            // Draw grid
            g.setColor(new Color(150, 150, 150, 100));
            for (int i = 0; i < width; i += 50) {
                g.drawLine(i, 0, i, height);
            }
            for (int i = 0; i < height; i += 50) {
                g.drawLine(0, i, width, i);
            }

            // Draw aircraft position
            int x = width / 2;
            int y = height / 2;

            g.setColor(new Color(255, 50, 50));
            int[] xPoints = {x, x - 15, x, x + 15};
            int[] yPoints = {y - 20, y + 10, y, y + 10};
            g.fillPolygon(xPoints, yPoints, 4);

            // Draw info box
            g.setColor(new Color(255, 255, 255, 230));
            g.fillRoundRect(10, 10, 300, 120, 10, 10);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(10, 10, 300, 120, 10, 10);

            // Info text
            g.setFont(new Font("Arial", Font.BOLD, 18));
            g.drawString("Volo: " + flightNumber, 20, 35);
            g.setFont(new Font("Arial", Font.PLAIN, 14));
            g.drawString(String.format("Posizione: %.4f°, %.4f°", lat, lon), 20, 60);
            g.drawString(String.format("Altitudine: %d m", altitude), 20, 85);
            g.drawString(String.format("Velocità: %d km/h", speed), 20, 110);

            g.dispose();

            File tempFile = File.createTempFile("live_tracking_", ".png");
            ImageIO.write(map, "PNG", tempFile);
            tempFile.deleteOnExit();

            return tempFile;
        } catch (IOException e) {
            logger.error("Error generating live tracking map", e);
            return null;
        }
    }

    private int lonToX(double lon, double minLon, double maxLon, int width) {
        return (int) ((lon - minLon) / (maxLon - minLon) * (width - 40)) + 20;
    }

    private int latToY(double lat, double minLat, double maxLat, int height) {
        return (int) ((maxLat - lat) / (maxLat - minLat) * (height - 40)) + 20;
    }
}

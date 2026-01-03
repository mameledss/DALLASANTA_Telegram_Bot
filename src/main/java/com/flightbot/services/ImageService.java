package com.flightbot.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;



public class ImageService {
    private static final Logger logger = Logger.getLogger(ImageService.class.getName());
    private final OkHttpClient client = new OkHttpClient();

    public File downloadLogoCompagnia(String airlineIata) {
        try {
            // Using a free airline logo service
            String url = String.format("https://images.kiwi.com/airlines/64/%s.png", airlineIata);

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return createDefaultLogo(airlineIata);
                }

                InputStream inputStream = response.body().byteStream();
                BufferedImage image = ImageIO.read(inputStream);

                File tempFile = File.createTempFile("airline_logo_", ".png");
                ImageIO.write(image, "PNG", tempFile);
                tempFile.deleteOnExit();

                return tempFile;
            }
        } catch (IOException e) {
            logger.severe(String.format("Errore download logo compagnia: %s", e.getMessage()));
            return createDefaultLogo(airlineIata);
        }
    }

    private File createDefaultLogo(String airlineIata) {
        try {
            int size = 64;
            BufferedImage logo = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = logo.createGraphics();

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Circle background
            g.setColor(new Color(41, 128, 185));
            g.fillOval(0, 0, size, size);

            // Text
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 20));
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(airlineIata);
            int textHeight = fm.getHeight();
            g.drawString(airlineIata, (size - textWidth) / 2, (size + textHeight) / 2 - 4);

            g.dispose();

            File tempFile = File.createTempFile("default_logo_", ".png");
            ImageIO.write(logo, "PNG", tempFile);
            tempFile.deleteOnExit();

            return tempFile;
        } catch (IOException e) {
            logger.severe(String.format("Errore creazione logo di default: %s", e.getMessage()));
            return null;
        }
    }

    public File scaricaImmagineAereo(String aircraftIdentifier) {
        try {
            // Check if the identifier is an ICAO24 (6-character hex)
            if (aircraftIdentifier != null && aircraftIdentifier.length() == 6 && aircraftIdentifier.matches("[0-9A-Fa-f]+")) {
                // It's an ICAO24, convert to registration using hexdb.io
                String registration = getRegistrationFromIcao24(aircraftIdentifier);
                if (registration != null) {
                    // Use the registration to get the photo
                    return downloadAircraftImageFromJetPhotos(registration, null);
                }
            }

            // If not ICAO24 or conversion failed, try as registration or aircraft type
            return downloadAircraftImageFromJetPhotos(aircraftIdentifier, null);
        } catch (Exception e) {
            logger.severe(String.format("Errore download immagine aereo: %s", e.getMessage()));
            return createAircraftPlaceholder(aircraftIdentifier);
        }
    }

    /**
     * Converte un codice ICAO24 in registration usando hexdb.io
     * @param icao24 Il codice ICAO24 (es. 40621D)
     * @return La registration dell'aereo o null se non trovata
     */
    private String getRegistrationFromIcao24(String icao24) {
        try {
            String url = "https://hexdb.io/api/v1/aircraft/" + icao24.toUpperCase();

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "FlightBot/1.0")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.fine(String.format("Registrazione non trovata per ICAO24: %s", icao24));
                    return null;
                }

                String jsonResponse = response.body().string();
                JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();

                if (jsonObject.has("Registration")) {
                    String registration = jsonObject.get("Registration").getAsString();
                    logger.info(String.format("ICAO24 %s convertito a registrazione %s", icao24, registration));
                    return registration;
                }
            }
        } catch (Exception e) {
            logger.severe(String.format("Errore conversione ICAO24 a registrazione: %s", e.getMessage()));
        }
        return null;
    }
    public File downloadAircraftImageFromJetPhotos(String registration, String aircraftType) {
        try {
            // Prima tentiamo con il registration number se disponibile
            if (registration != null && !registration.isEmpty()) {
                File image = tryDownloadFromJetPhotos(registration);
                if (image != null) {
                    return image;
                }
            }

            // Se non funziona, proviamo con il tipo di aereo
            if (aircraftType != null && !aircraftType.isEmpty()) {
                File image = tryDownloadFromJetPhotos(aircraftType);
                if (image != null) {
                    return image;
                }
            }

            // Se tutto fallisce, restituiamo un placeholder
            return createAircraftPlaceholder(aircraftType != null ? aircraftType : registration);
        } catch (Exception e) {
            logger.severe(String.format("Errore download immagine aereo da JetPhotos: %s", e.getMessage()));
            return createAircraftPlaceholder(aircraftType != null ? aircraftType : registration);
        }
    }

    private File tryDownloadFromJetPhotos(String query) {
        try {
            // JetPhotos non ha un'API pubblica diretta, ma possiamo provare a cercare immagini
            // usando altri servizi come Planespotters.net API o simili
            // Per ora usiamo un approccio con URL diretti se disponibili

            // Determina se il query è un codice ICAO24 (hex) o una registrazione
            boolean isHexCode = query != null && query.length() == 6 && query.matches("[0-9A-Fa-f]+");
            String endpoint = isHexCode ? "hex" : "reg";

            // Proviamo con Planespotters.net che ha foto reali
            String url = String.format("https://api.planespotters.net/pub/photos/%s/%s", endpoint, query);

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.fine(String.format("Nessuna foto trovata su planespotters per: %s", query));
                    return null;
                }

                // Parse JSON response per ottenere l'URL dell'immagine
                String jsonResponse = response.body().string();

                // Usa Gson per parsing JSON corretto
                JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();

                if (jsonObject.has("photos")) {
                    JsonArray photos = jsonObject.getAsJsonArray("photos");
                    if (photos.size() > 0) {
                        JsonObject photo = photos.get(0).getAsJsonObject();
                        if (photo.has("thumbnail")) {
                            JsonObject thumbnail = photo.getAsJsonObject("thumbnail");
                            if (thumbnail.has("src")) {
                                String imageUrl = thumbnail.get("src").getAsString();
                                return downloadImageFromUrl(imageUrl);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.fine(String.format("Errore download da JetPhotos/Planespotters: %s", e.getMessage()));
        }
        return null;
    }

    private File downloadImageFromUrl(String imageUrl) {
        try {
            Request request = new Request.Builder()
                    .url(imageUrl)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return null;
                }

                InputStream inputStream = response.body().byteStream();
                BufferedImage image = ImageIO.read(inputStream);

                if (image == null) {
                    return null;
                }

                File tempFile = File.createTempFile("aircraft_photo_", ".jpg");
                ImageIO.write(image, "JPG", tempFile);
                tempFile.deleteOnExit();

                logger.info("Successfully downloaded aircraft image");
                return tempFile;
            }
        } catch (IOException e) {
            logger.severe(String.format("Errore download immagine da url: %s", imageUrl));
            return null;
        }
    }

    private File createAircraftPlaceholder(String aircraftType) {
        try {
            int width = 400;
            int height = 300;
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Sky background
            GradientPaint gradient = new GradientPaint(0, 0, new Color(135, 206, 235),
                    0, height, new Color(176, 224, 230));
            g.setPaint(gradient);
            g.fillRect(0, 0, width, height);

            // Simplified aircraft silhouette
            g.setColor(new Color(80, 80, 80));
            int[] xPoints = {width/2 - 100, width/2 - 20, width/2 + 20, width/2 + 100};
            int[] yPoints = {height/2 + 20, height/2 - 40, height/2 - 40, height/2 + 20};
            g.fillPolygon(xPoints, yPoints, 4);

            // Aircraft type text
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            String text = aircraftType != null ? aircraftType : "Aircraft";
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            g.drawString(text, (width - textWidth) / 2, height - 30);

            g.dispose();

            File tempFile = File.createTempFile("aircraft_", ".png");
            ImageIO.write(image, "PNG", tempFile);
            tempFile.deleteOnExit();

            return tempFile;
        } catch (IOException e) {
            logger.severe(String.format("Errore creazione placeholder aereo: %s", e.getMessage()));
            return null;
        }
    }
}

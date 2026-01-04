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

    public File scaricaLogoCompagnia(String iataCompagnia) {
        try {
            String url = String.format("https://images.kiwi.com/airlines/64/%s.png", iataCompagnia);

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return creaLogoDefaultCompagnia(iataCompagnia);
                }
                InputStream inputStream = response.body().byteStream(); //legge i byte della foto in input
                BufferedImage immagine = ImageIO.read(inputStream); //rappresentazione della foto

                File tempFile = File.createTempFile("airline_logo_", ".png"); //crea file temporaneo della foto con nome random
                ImageIO.write(immagine, "PNG", tempFile); //scrive i dati dell'immagine nel file appena creato
                tempFile.deleteOnExit(); //cancella il file al termine del programma

                return tempFile;
            }
        } catch (IOException e) {
            logger.severe(String.format("Errore download logo compagnia: %s", e.getMessage()));
            return creaLogoDefaultCompagnia(iataCompagnia);
        }
    }

    private File creaLogoDefaultCompagnia(String iataCompagnia) {
        try {
            int dimensione = 64;
            BufferedImage logo = new BufferedImage(dimensione, dimensione, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = logo.createGraphics();

            //sfondo nero
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, dimensione, dimensione);

            //testo bianco
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 10));

            g.drawString(iataCompagnia, 5, 25); //disegna codice IATA

            g.drawString("default logo", 5, 40); //disegna "default logo"

            g.dispose();

            File tempFile = File.createTempFile("default_logo_", ".png"); //crea file temporaneo della foto con nome random
            ImageIO.write(logo, "PNG", tempFile); //scrive i dati dell'immagine nel file appena creato
            tempFile.deleteOnExit(); //cancella il file al termine del programma

            return tempFile;
        } catch (IOException e) {
            logger.severe(String.format("Errore creazione logo di default: %s", e.getMessage()));
            return null;
        }
    }

    public File scaricaImmagineAereo(String idAereo) {
        try {
            //se l'id aereo è un ICAO24
            if (idAereo != null && idAereo.length() == 6 && idAereo.matches("[0-9A-Fa-f]+")) { //uno o più caratteri da 0 a 9, da A a F, da a a f
                String registrazione = daIcao24ARegistrazione(idAereo); //lo converte a registrazione usando hexdb.io
                if (registrazione != null)
                    return scaricaDaPlaneSpotters(registrazione, null); //usa la registration per la foto
            }
            return scaricaDaPlaneSpotters(idAereo, null); //se non ha icao24 o la conversione ha fallito, prova con la registrazione o il tipo di aereo
        } catch (Exception e) {
            logger.severe(String.format("Errore download immagine aereo: %s", e.getMessage()));
            return creaLogoDefaultAereo(idAereo);
        }
    }

    private String daIcao24ARegistrazione(String icao24) {
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
                    String registrazione = jsonObject.get("Registration").getAsString();
                    logger.info(String.format("ICAO24 %s convertito a registrazione %s", icao24, registrazione));
                    return registrazione;
                }
            }
        } catch (Exception e) {
            logger.severe(String.format("Errore conversione ICAO24 a registrazione: %s", e.getMessage()));
        }
        return null;
    }

    public File scaricaDaPlaneSpotters(String registrazione, String tipoAereo) {
        try {
            if (registrazione != null && !registrazione.isEmpty()) { //prima tenta con registrazione se disponibile
                File immagine = provaDownloadDaPlaneSpotters(registrazione);

                if (immagine != null) return immagine;
            }

            if (tipoAereo != null && !tipoAereo.isEmpty()) { //se non funziona, prova con tipo aereo
                File immagine = provaDownloadDaPlaneSpotters(tipoAereo);

                if (immagine != null) return immagine;
            }

            //se tutto fallisce, restituisce un placeholder
            return creaLogoDefaultAereo(tipoAereo != null ? tipoAereo : registrazione);
        } catch (Exception e) {
            logger.severe(String.format("Errore download immagine aereo da JetPhotos: %s", e.getMessage()));
            return creaLogoDefaultAereo(tipoAereo != null ? tipoAereo : registrazione);
        }
    }

    private File provaDownloadDaPlaneSpotters(String query) {
        try {
            //determina se query è un codice ICAO24 (hex) o una registrazione
            boolean isEsadecimale = query != null && query.length() == 6 && query.matches("[0-9A-Fa-f]+");
            String endpoint = isEsadecimale ? "hex" : "reg";

            String url = String.format("https://api.planespotters.net/pub/photos/%s/%s", endpoint, query);

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.fine(String.format("Nessuna foto trovata su planespotters per: %s", query));
                    return null;
                }

                String jsonResponse = response.body().string();

                JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();

                if (jsonObject.has("photos")) {
                    JsonArray foto = jsonObject.getAsJsonArray("photos");
                    if (!foto.isEmpty()) {
                        JsonObject photo = foto.get(0).getAsJsonObject();
                        if (photo.has("thumbnail")) {
                            JsonObject thumbnail = photo.getAsJsonObject("thumbnail");
                            if (thumbnail.has("src")) {
                                String urlImmagine = thumbnail.get("src").getAsString(); //ottiene l'url della foto
                                return scaricaImmagineDaUrl(urlImmagine);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.fine(String.format("Errore download da Planespotters: %s", e.getMessage()));
        }
        return null;
    }

    private File scaricaImmagineDaUrl(String urlImmagine) {
        try {
            Request request = new Request.Builder()
                    .url(urlImmagine)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) return null;

                InputStream inputStream = response.body().byteStream();
                BufferedImage immagine = ImageIO.read(inputStream);

                if (immagine == null) return null;

                File tempFile = File.createTempFile("aircraft_photo_", ".jpg");
                ImageIO.write(immagine, "JPG", tempFile);
                tempFile.deleteOnExit();

                logger.info("Immagine aereo scaricata con successo");
                return tempFile;
            }
        } catch (IOException e) {
            logger.severe(String.format("Errore download immagine da url: %s", urlImmagine));
            return null;
        }
    }

    private File creaLogoDefaultAereo(String tipoAereo) {
        try {
            int larg = 400;
            int alt = 300;
            BufferedImage immagine = new BufferedImage(larg, alt, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = immagine.createGraphics();

            //sfondo nero
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, larg, alt);

            //testo bianco
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 20));

            g.drawString(tipoAereo != null ? tipoAereo : "Aircraft", 20, 140); //disegna tipo aereo

            g.drawString("default logo", 20, 170); //disegna "default logo"

            g.dispose();

            File tempFile = File.createTempFile("aircraft_", ".png");
            ImageIO.write(immagine, "PNG", tempFile);
            tempFile.deleteOnExit();

            return tempFile;
        } catch (IOException e) {
            logger.severe(String.format("Errore creazione placeholder aereo: %s", e.getMessage()));
            return null;
        }
    }
}
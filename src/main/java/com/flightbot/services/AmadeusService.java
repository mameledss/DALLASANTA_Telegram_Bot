package com.flightbot.services;

import com.flightbot.config.ConfigLoader;
import com.flightbot.models.Ticket;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class AmadeusService {
    private static final Logger logger = Logger.getLogger(AmadeusService.class.getName());
    private final OkHttpClient client = new OkHttpClient();
    private final String apiKey = ConfigLoader.getAmadeusApiKey();
    private final String apiSecret = ConfigLoader.getAmadeusApiSecret();
    private String accessToken;
    private long tokenExpiry = 0;

    public List<Ticket> cercaOfferteVolo(String origine, String destinazione, String dataPartenza, int adulti) {
        List<Ticket> biglietti = new ArrayList<>();

        try {
            ensureValidToken();

            String url = String.format(
                    "https://test.api.amadeus.com/v2/shopping/flight-offers?originLocationCode=%s&destinationLocationCode=%s&departureDate=%s&adults=%d&max=5",
                    origine, destinazione, dataPartenza, adulti
            );

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.severe(String.format("Errore ricerca offerte di volo: %s", response.code()));
                    return biglietti;
                }

                String jsonResponse = response.body().string();
                JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();
                JsonArray data = jsonObject.getAsJsonArray("data");

                if (data != null) {
                    for (int i = 0; i < data.size(); i++) {
                        JsonObject offerta = data.get(i).getAsJsonObject();
                        Ticket biglietto = parseTicketFromJson(offerta);
                        if (biglietto != null) {
                            biglietti.add(biglietto);
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.severe(String.format("Errore ricerca offerte di volo: %s", e.getMessage()));
        }
        return biglietti;
    }

    private Ticket parseTicketFromJson(JsonObject offer) {
        try {
            Ticket ticket = new Ticket();

            // Prezzo e valuta
            if (offer.has("price")) {
                JsonObject price = offer.getAsJsonObject("price");
                ticket.setPrezzoTotale(price.get("total").getAsString());
                ticket.setValuta(price.get("currency").getAsString());
            }

            // ID offerta
            if (offer.has("id")) {
                ticket.setIdOfferta(offer.get("id").getAsString());
            }

            // Posti disponibili
            if (offer.has("numberOfBookableSeats")) {
                ticket.setPostiDisponibili(offer.get("numberOfBookableSeats").getAsInt());
            }

            // Itinerari
            var itineraries = offer.getAsJsonArray("itineraries");
            if (itineraries != null && !itineraries.isEmpty()) {
                var firstItinerary = itineraries.get(0).getAsJsonObject();
                var segments = firstItinerary.getAsJsonArray("segments");

                if (segments != null) {
                    // Numero di scali
                    int stops = segments.size() - 1;
                    ticket.setNumeroScali(stops);

                    // Durata totale
                    if (firstItinerary.has("duration")) {
                        ticket.setDurataTotale(firstItinerary.get("duration").getAsString());
                    }

                    // Info primo e ultimo segmento
                    var firstSegment = segments.get(0).getAsJsonObject();
                    var lastSegment = segments.get(segments.size() - 1).getAsJsonObject();

                    // Orari
                    if (firstSegment.has("departure")) {
                        var departure = firstSegment.getAsJsonObject("departure");
                        if (departure.has("at")) {
                            ticket.setOrarioPartenza(departure.get("at").getAsString());
                        }
                    }

                    if (lastSegment.has("arrival")) {
                        var arrival = lastSegment.getAsJsonObject("arrival");
                        if (arrival.has("at")) {
                            ticket.setOrarioArrivo(arrival.get("at").getAsString());
                        }
                    }

                    // Compagnia aerea e numero volo
                    if (firstSegment.has("carrierCode")) {
                        ticket.setCodiceCompagnia(firstSegment.get("carrierCode").getAsString());
                    }
                    if (firstSegment.has("number")) {
                        ticket.setNumeroVolo(firstSegment.get("number").getAsString());
                    }
                }
            }

            return ticket;
        } catch (Exception e) {
            logger.severe(String.format("Errore parsing JSON: %s", e.getMessage()));
            return null;
        }
    }

    private void ensureValidToken() throws IOException {
        long currentTime = System.currentTimeMillis() / 1000;

        if (accessToken == null || currentTime >= tokenExpiry) {
            getAccessToken();
        }
    }

    private void getAccessToken() throws IOException {
        RequestBody formBody = new FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("client_id", apiKey)
                .add("client_secret", apiSecret)
                .build();

        Request request = new Request.Builder()
                .url("https://test.api.amadeus.com/v1/security/oauth2/token")
                .post(formBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get access token: " + response.code());
            }

            String jsonResponse = response.body().string();
            JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();

            accessToken = jsonObject.get("access_token").getAsString();
            int expiresIn = jsonObject.get("expires_in").getAsInt();
            tokenExpiry = (System.currentTimeMillis() / 1000) + expiresIn - 60; // 60 seconds buffer

            logger.info("Amadeus access token obtained successfully");
        }
    }
}

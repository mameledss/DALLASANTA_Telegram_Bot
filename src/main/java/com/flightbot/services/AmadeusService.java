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
        List<Ticket> biglietti = new ArrayList<>(); //lista di biglietti trovati

        try {
            verificaToken(); //verifica se il token di accesso è valido perché usa OAuth2

            String url = String.format(
                    "https://test.api.amadeus.com/v2/shopping/flight-offers?originLocationCode=%s&destinationLocationCode=%s&departureDate=%s&adults=%d&max=5",
                    origine, destinazione, dataPartenza, adulti
            ); //max 5 offerte

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken) //aggiunge il token per OAuth2
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.severe(String.format("Errore ricerca offerte di volo: %s", response.code()));
                    return biglietti;
                }

                String jsonResponse = response.body().string();
                JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();
                JsonArray data = jsonObject.getAsJsonArray("data");

                if (data != null) { //se data contiene offerte
                    for (int i = 0; i < data.size(); i++) { //per ogni offerta
                        JsonObject offerta = data.get(i).getAsJsonObject(); //prende l'offerta a i
                        Ticket biglietto = daJsonATicket(offerta); //converte in oggetto Ticket
                        if (biglietto != null)
                            biglietti.add(biglietto); //aggiunge alla list il biglietto
                    }
                }
            }
        } catch (IOException e) {
            logger.severe(String.format("Errore ricerca offerte di volo: %s", e.getMessage()));
        }
        return biglietti;
    }

    private Ticket daJsonATicket(JsonObject offerta) {
        try {
            Ticket biglietto = new Ticket();

            if (offerta.has("price")) { //se ha il campo prezzo
                JsonObject prezzo = offerta.getAsJsonObject("price");
                biglietto.setPrezzoTotale(prezzo.get("total").getAsString());
                biglietto.setValuta(prezzo.get("currency").getAsString());
            }

            if (offerta.has("id"))
                biglietto.setIdOfferta(offerta.get("id").getAsString());

            if (offerta.has("numberOfBookableSeats")) {
                biglietto.setPostiDisponibili(offerta.get("numberOfBookableSeats").getAsInt());
            }
            //vedere readme per struttura JSON
            JsonArray itinerari = offerta.getAsJsonArray("itineraries"); //array di itinerari
            if (itinerari != null && !itinerari.isEmpty()) {
                JsonObject primoItinerario = itinerari.get(0).getAsJsonObject(); //primo itinerario
                JsonArray segmenti = primoItinerario.getAsJsonArray("segments"); //vari segmenti dell'itinerario (i segmenti sono i voli)

                if (segmenti != null) {
                    int scali = segmenti.size() - 1; //numero di scali
                    biglietto.setNumeroScali(scali);

                    if (primoItinerario.has("duration")) //durata totale
                        biglietto.setDurataTotale(primoItinerario.get("duration").getAsString());

                    //info primo e ultimo segmento
                    JsonObject primoSegmento = segmenti.get(0).getAsJsonObject();
                    JsonObject ultimoSegmento = segmenti.get(segmenti.size() - 1).getAsJsonObject();

                    if (primoSegmento.has("departure")) {
                        JsonObject partenza = primoSegmento.getAsJsonObject("departure");
                        if (partenza.has("at"))
                            biglietto.setOrarioPartenza(partenza.get("at").getAsString());
                    }

                    if (ultimoSegmento.has("arrival")) {
                        JsonObject arrivo = ultimoSegmento.getAsJsonObject("arrival");
                        if (arrivo.has("at"))
                            biglietto.setOrarioArrivo(arrivo.get("at").getAsString());
                    }

                    if (primoSegmento.has("carrierCode")) //compagnia aerea
                        biglietto.setCodiceCompagnia(primoSegmento.get("carrierCode").getAsString());

                    if (primoSegmento.has("number")) //numero volo
                        biglietto.setNumeroVolo(primoSegmento.get("number").getAsString());
                }
            }
            return biglietto;
        } catch (Exception e) {
            logger.severe(String.format("Errore parsing JSON: %s", e.getMessage()));
            return null;
        }
    }

    private void verificaToken() throws IOException {
        long tempoCorrente = System.currentTimeMillis() / 1000; //ottiene il tempo corrente in secondi

        if (accessToken == null || tempoCorrente >= tokenExpiry) //se non esiste un token o il token è scaduto
            getToken(); //ottiene un nuovo token
    }

    private void getToken() throws IOException { //prepara richiesta OAuth2
        RequestBody formBody = new FormBody.Builder()
                .add("grant_type", "client_credentials") //tipo di autenticazione, client_credentials=autenticazione server-to-server
                .add("client_id", apiKey) //chiave API Amadeus
                .add("client_secret", apiSecret) //password API Amadeus
                .build();

        Request request = new Request.Builder() //chiamata POST all'endpoint di autenticazione di Amadeus
                .url("https://test.api.amadeus.com/v1/security/oauth2/token")
                .post(formBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful())
                throw new IOException("Failed to get access token: " + response.code());

            String jsonResponse = response.body().string();
            JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();

            accessToken = jsonObject.get("access_token").getAsString();
            int expiresIn = jsonObject.get("expires_in").getAsInt();
            tokenExpiry = (System.currentTimeMillis() / 1000) + expiresIn - 60;
            //calcola quando scade (tempo corrente + durata - 60 secondi di buffer)
            //buffer di 60 secondi evita che token scada durante una richiesta
            logger.info("Token Amadeus ottenuto correttamente");
        }
    }
}
package com.flightbot.services;

import com.flightbot.config.ConfigLoader;
import com.flightbot.models.Flight;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.util.logging.Logger;
import java.io.IOException;

public class FlightService {
    private static final Logger logger = Logger.getLogger(FlightService.class.getName());
    private final OkHttpClient client = new OkHttpClient();
    private final String apiKey = ConfigLoader.getAviationStackApiKey();

    public Flight getInfoVolo(String flightNumber) {
        try {
            String url = String.format(
                    "http://api.aviationstack.com/v1/flights?access_key=%s&flight_iata=%s",
                    apiKey, flightNumber
            );

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.severe(String.format("Errore nell'ottenere info volo: %s", response.code()));
                    return null;
                }

                String jsonResponse = response.body().string();
                JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();
                JsonArray data = jsonObject.getAsJsonArray("data");

                if (data.isEmpty()) return null;

                return daJsonAFlight(data.get(0).getAsJsonObject()); //prende solo il primo volo
            }
        } catch (IOException e) {
            logger.severe(String.format("Errore nell'ottenere info volo: %s", e.getMessage()));
            return null;
        }
    }

    private Flight daJsonAFlight(JsonObject json) {
        try {
            Flight flight = new Flight();

            JsonObject volo = getJsonObjectONull(json, "flight");
            if (volo != null) {
                flight.setNumeroVolo(getStringONull(volo, "iata"));
                flight.setIataCompagnia(getStringONull(volo, "iata") != null ? getStringONull(volo, "iata").substring(0, 2) : null); //prima due lettere->IATA compagnia
            }

            JsonObject compagnia = getJsonObjectONull(json, "airline");
            if (compagnia != null)
                flight.setCompagnia(getStringONull(compagnia, "name"));

            flight.setStato(getStringONull(json, "flight_status"));

            JsonObject partenza = getJsonObjectONull(json, "departure");
            if (partenza != null) {
                flight.setAeroportoPartenza(getStringONull(partenza, "airport"));
                flight.setIataPartenza(getStringONull(partenza, "iata"));
                flight.setOrarioPartenza(getStringONull(partenza, "actual"));
                flight.setPartenzaProgrammata(getStringONull(partenza, "scheduled"));
                flight.setTerminal(getStringONull(partenza, "terminal"));
                flight.setGate(getStringONull(partenza, "gate"));

                if (partenza.get("delay") != null && !partenza.get("delay").isJsonNull())
                    flight.setRitardo(partenza.get("delay").getAsInt());
            }

            JsonObject arrivo = getJsonObjectONull(json, "arrival");
            if (arrivo != null) {
                flight.setAeroportoArrivo(getStringONull(arrivo, "airport"));
                flight.setIataArrivo(getStringONull(arrivo, "iata"));
                flight.setOrarioArrivo(getStringONull(arrivo, "actual"));
                flight.setArrivoProgrammato(getStringONull(arrivo, "scheduled"));
            }

            JsonObject aereo = getJsonObjectONull(json, "aircraft");
            if (aereo != null) {
                flight.setRegistrazioneAereo(getStringONull(aereo, "registration"));
                flight.setTipoAereo(getStringONull(aereo, "iata"));
            }

            JsonObject datiLive = getJsonObjectONull(json, "live");
            if (datiLive != null) {
                flight.setIcao24(getStringONull(datiLive, "hex"));
                if (datiLive.get("latitude") != null && !datiLive.get("latitude").isJsonNull())
                    flight.setLatitudine(datiLive.get("latitude").getAsDouble());

                if (datiLive.get("longitude") != null && !datiLive.get("longitude").isJsonNull())
                    flight.setLongitudine(datiLive.get("longitude").getAsDouble());

                if (datiLive.get("altitude") != null && !datiLive.get("altitude").isJsonNull())
                    flight.setAltitudine(datiLive.get("altitude").getAsInt());

                if (datiLive.get("speed_horizontal") != null && !datiLive.get("speed_horizontal").isJsonNull())
                    flight.setVelocita(datiLive.get("speed_horizontal").getAsInt());
            }
            return flight;
        } catch (Exception e) {
            logger.severe(String.format("Errore parsing JSON volo: %s", e.getMessage()));
            return null;
        }
    }

    private String getStringONull(JsonObject obj, String key) {
        JsonElement elemento = obj.get(key);
        return (elemento != null && !elemento.isJsonNull()) ? elemento.getAsString() : null; //controlla che non sia null
    }

    private JsonObject getJsonObjectONull(JsonObject obj, String key) {
        JsonElement element = obj.get(key);
        return (element != null && !element.isJsonNull() && element.isJsonObject()) ? element.getAsJsonObject() : null;
    }
}
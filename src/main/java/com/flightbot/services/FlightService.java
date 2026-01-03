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

                if (data.size() == 0) {
                    return null;
                }

                return parseFlightFromJson(data.get(0).getAsJsonObject());
            }
        } catch (IOException e) {
            logger.severe(String.format("Errore nell'ottenere info volo: %s", e.getMessage()));
            return null;
        }
    }

    private Flight parseFlightFromJson(JsonObject json) {
        try {
            Flight flight = new Flight();

            // Flight info
            JsonObject flightObj = getJsonObjectOrNull(json, "flight");
            if (flightObj != null) {
                flight.setNumeroVolo(getStringOrNull(flightObj, "iata"));
                flight.setIataCompagnia(getStringOrNull(flightObj, "iata") != null ?
                        getStringOrNull(flightObj, "iata").substring(0, 2) : null);
            }

            // Airline
            JsonObject airlineObj = getJsonObjectOrNull(json, "airline");
            if (airlineObj != null) {
                flight.setCompagnia(getStringOrNull(airlineObj, "name"));
            }

            // Flight status
            flight.setStato(getStringOrNull(json, "flight_status"));

            // Departure
            JsonObject departureObj = getJsonObjectOrNull(json, "departure");
            if (departureObj != null) {
                flight.setAeroportoPartenza(getStringOrNull(departureObj, "airport"));
                flight.setIataPartenza(getStringOrNull(departureObj, "iata"));
                flight.setOrarioPartenza(getStringOrNull(departureObj, "actual"));
                flight.setPartenzaProgrammata(getStringOrNull(departureObj, "scheduled"));
                flight.setTerminal(getStringOrNull(departureObj, "terminal"));
                flight.setGate(getStringOrNull(departureObj, "gate"));

                if (departureObj.get("delay") != null && !departureObj.get("delay").isJsonNull()) {
                    flight.setRitardo(departureObj.get("delay").getAsInt());
                }
            }

            // Arrival
            JsonObject arrivalObj = getJsonObjectOrNull(json, "arrival");
            if (arrivalObj != null) {
                flight.setAeroportoArrivo(getStringOrNull(arrivalObj, "airport"));
                flight.setIataArrivo(getStringOrNull(arrivalObj, "iata"));
                flight.setOrarioArrivo(getStringOrNull(arrivalObj, "actual"));
                flight.setArrivoProgrammato(getStringOrNull(arrivalObj, "scheduled"));
            }

            // Aircraft
            JsonObject aircraftObj = getJsonObjectOrNull(json, "aircraft");
            if (aircraftObj != null) {
                flight.setRegistrazioneAereo(getStringOrNull(aircraftObj, "registration"));
                flight.setTipoAereo(getStringOrNull(aircraftObj, "iata"));
            }

            // Live data
            JsonObject liveObj = getJsonObjectOrNull(json, "live");
            if (liveObj != null) {
                flight.setIcao24(getStringOrNull(liveObj, "hex"));
                if (liveObj.get("latitude") != null && !liveObj.get("latitude").isJsonNull()) {
                    flight.setLatitudine(liveObj.get("latitude").getAsDouble());
                }
                if (liveObj.get("longitude") != null && !liveObj.get("longitude").isJsonNull()) {
                    flight.setLongitudine(liveObj.get("longitude").getAsDouble());
                }
                if (liveObj.get("altitude") != null && !liveObj.get("altitude").isJsonNull()) {
                    flight.setAltitudine(liveObj.get("altitude").getAsInt());
                }
                if (liveObj.get("speed_horizontal") != null && !liveObj.get("speed_horizontal").isJsonNull()) {
                    flight.setVelocita(liveObj.get("speed_horizontal").getAsInt());
                }
            }

            return flight;
        } catch (Exception e) {
            logger.severe(String.format("Errore parsing JSON volo: %s", e.getMessage()));
            return null;
        }
    }

    private String getStringOrNull(JsonObject obj, String key) {
        JsonElement element = obj.get(key);
        return (element != null && !element.isJsonNull()) ? element.getAsString() : null;
    }

    private JsonObject getJsonObjectOrNull(JsonObject obj, String key) {
        JsonElement element = obj.get(key);
        return (element != null && !element.isJsonNull() && element.isJsonObject()) ? element.getAsJsonObject() : null;
    }
}

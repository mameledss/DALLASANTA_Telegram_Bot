package com.flightbot.services;

import com.flightbot.config.ConfigLoader;
import com.flightbot.models.Airport;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.util.logging.Logger;
import java.io.IOException;

public class AirportService {
    private static final Logger logger = Logger.getLogger(AirportService.class.getName());
    private final OkHttpClient client = new OkHttpClient();
    private final String apiKey = ConfigLoader.getAeroDataBoxApiKey();

    public Airport getInfoAeroportoServ(String iataCode) {
        try {
            String url = String.format("https://aerodatabox.p.rapidapi.com/airports/iata/%s", iataCode);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("X-RapidAPI-Key", apiKey)
                    .addHeader("X-RapidAPI-Host", "aerodatabox.p.rapidapi.com")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.severe(String.format("Errore info aeroporto: %s", response.code()));
                    return null;
                }

                String jsonResponse = response.body().string(); //risposta json trasformata in stringa
                JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject(); //trasforma stringa in oggetto json

                return parseAirportFromJson(jsonObject); //esegue parsing dell'oggetto json
            }
        } catch (IOException e) {
            logger.severe(String.format("Errore info aeroporto: %s", e.getMessage()));
            return null;
        }
    }

    private Airport parseAirportFromJson(JsonObject json) {
        try {
            Airport aeroporto = new Airport(); //crea un nuovo oggetto Airport

            //usa fullName se disponibile, altrimenti shortName
            String nome = getStringOrNull(json, "fullName");
            if (nome == null)
                nome = getStringOrNull(json, "shortName");

            aeroporto.setNome(nome);

            aeroporto.setCodiceIata(getStringOrNull(json, "iata"));
            aeroporto.setCodiceIcao(getStringOrNull(json, "icao"));

            JsonObject objPosizione = getJsonObjectOrNull(json, "location");
            if (objPosizione != null) {
                if (objPosizione.get("lat") != null && !objPosizione.get("lat").isJsonNull())
                    aeroporto.setLatitudine(objPosizione.get("lat").getAsDouble());

                if (objPosizione.get("lon") != null && !objPosizione.get("lon").isJsonNull())
                    aeroporto.setLongitudine(objPosizione.get("lon").getAsDouble());
            }

            aeroporto.setCitta(getStringOrNull(json, "municipalityName"));

            JsonObject objPaese = getJsonObjectOrNull(json, "country");
            if (objPaese != null)
                aeroporto.setPaese(getStringOrNull(objPaese, "name"));

            aeroporto.setFuso(getStringOrNull(json, "timeZone"));

            JsonObject urlsObj = getJsonObjectOrNull(json, "urls");
            if (urlsObj != null) {
                aeroporto.setSito(getStringOrNull(urlsObj, "webSite"));
                aeroporto.setGoogleMaps(getStringOrNull(urlsObj, "googleMaps"));
            }
            return aeroporto;
        } catch (Exception e) {
            logger.severe(String.format("Errore info aeroporto: %s", e.getMessage()));
            return null;
        }
    }

    private String getStringOrNull(JsonObject obj, String key) {
        JsonElement elemento = obj.get(key);
        return (elemento != null && !elemento.isJsonNull()) ? elemento.getAsString() : null;
    }

    private JsonObject getJsonObjectOrNull(JsonObject obj, String key) {
        JsonElement elemento = obj.get(key);
        return (elemento != null && !elemento.isJsonNull() && elemento.isJsonObject()) ? elemento.getAsJsonObject() : null;
    }
}
package com.flightbot.services;

import com.flightbot.config.ConfigLoader;
import com.flightbot.models.Weather;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.util.logging.Logger;
import java.io.IOException;

public class WeatherService {
    private static final Logger logger = Logger.getLogger(WeatherService.class.getName());
    private final OkHttpClient client = new OkHttpClient();
    private final String apiKey = ConfigLoader.getOpenWeatherApiKey();

    public Weather getMeteo(String city) {
        try {
            String url = String.format(
                    "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric&lang=it",
                    city, apiKey
            );

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.severe(String.format("Errore ottenimento meteo: %d", response.code()));
                    return null;
                }

                String jsonResponse = response.body().string();
                JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();

                return daJsonAWeather(jsonObject);
            }
        } catch (IOException e) {
            logger.severe(String.format("Errore ottenimento meteo: %s", e.getMessage()));
            return null;
        }
    }

    public Weather getMeteoDaCoord(double lat, double lon) {
        try {
            String url = String.format(
                    "https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&appid=%s&units=metric&lang=it",
                    lat, lon, apiKey
            );

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.severe(String.format("Errore ottenimento meteo da coordinate: %d", response.code()));
                    return null;
                }

                String jsonResponse = response.body().string();
                JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();

                return daJsonAWeather(jsonObject);
            }
        } catch (IOException e) {
            logger.severe(String.format("Errore ottenimento meteo da coordinate: %s", e.getMessage()));
            return null;
        }
    }

    private Weather daJsonAWeather(JsonObject json) {
        try {
            Weather meteo = new Weather();

            JsonElement nome = json.get("name");
            if (nome != null && !nome.isJsonNull())
                meteo.setCitta(nome.getAsString());

            JsonArray arrayMeteo = json.getAsJsonArray("weather");
            if (arrayMeteo != null && !arrayMeteo.isEmpty()) {
                JsonElement primoElemento = arrayMeteo.get(0);
                if (primoElemento != null && primoElemento.isJsonObject()) {
                    JsonObject meteoObj = primoElemento.getAsJsonObject();
                    JsonElement descrizione = meteoObj.get("description");
                    JsonElement icona = meteoObj.get("icon");

                    if (descrizione != null && !descrizione.isJsonNull())
                        meteo.setDescrizione(descrizione.getAsString());

                    if (icona != null && !icona.isJsonNull())
                        meteo.setIcona(icona.getAsString());
                }
            }

            JsonObject main = getJsonObjectONull(json, "main");
            if (main != null) {
                meteo.setTemperatura(main.get("temp").getAsDouble());
                meteo.setPercepita(main.get("feels_like").getAsDouble());
                meteo.setUmidita(main.get("humidity").getAsInt());
                meteo.setPressione(main.get("pressure").getAsInt());
            }

            JsonObject vento = getJsonObjectONull(json, "wind");
            if (vento != null) {
                JsonElement speedElement = vento.get("speed");
                if (speedElement != null && !speedElement.isJsonNull())
                    meteo.setVelocitaVento(speedElement.getAsDouble());
            }

            JsonElement visibilita = json.get("visibility");
            if (visibilita != null && !visibilita.isJsonNull())
                meteo.setVisibilita(visibilita.getAsInt());

            return meteo;
        } catch (Exception e) {
            logger.severe(String.format("Errore parsing meteo JSON: %s", e.getMessage()));
            return null;
        }
    }

    private JsonObject getJsonObjectONull(JsonObject obj, String key) {
        JsonElement elemento = obj.get(key);
        return (elemento != null && !elemento.isJsonNull() && elemento.isJsonObject()) ? elemento.getAsJsonObject() : null;
    }
}
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
                    logger.severe(String.format("Failed to get weather: %d", response.code()));
                    return null;
                }

                String jsonResponse = response.body().string();
                JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();

                return parseWeatherFromJson(jsonObject);
            }
        } catch (IOException e) {
            logger.severe(String.format("Error getting weather: %s", e.getMessage()));
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
                    logger.severe(String.format("Failed to get weather by coordinates: %d", response.code()));
                    return null;
                }

                String jsonResponse = response.body().string();
                JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();

                return parseWeatherFromJson(jsonObject);
            }
        } catch (IOException e) {
            logger.severe(String.format("Error getting weather by coordinates: %s", e.getMessage()));
            return null;
        }
    }

    private Weather parseWeatherFromJson(JsonObject json) {
        try {
            Weather weather = new Weather();

            JsonElement nameElement = json.get("name");
            if (nameElement != null && !nameElement.isJsonNull()) {
                weather.setCitta(nameElement.getAsString());
            }

            JsonArray weatherArray = json.getAsJsonArray("weather");
            if (weatherArray != null && weatherArray.size() > 0) {
                JsonElement firstElement = weatherArray.get(0);
                if (firstElement != null && firstElement.isJsonObject()) {
                    JsonObject weatherObj = firstElement.getAsJsonObject();
                    JsonElement desc = weatherObj.get("description");
                    JsonElement icon = weatherObj.get("icon");
                    if (desc != null && !desc.isJsonNull()) {
                        weather.setDescrizione(desc.getAsString());
                    }
                    if (icon != null && !icon.isJsonNull()) {
                        weather.setIcona(icon.getAsString());
                    }
                }
            }

            JsonObject mainObj = getJsonObjectOrNull(json, "main");
            if (mainObj != null) {
                weather.setTemperatura(mainObj.get("temp").getAsDouble());
                weather.setPercepita(mainObj.get("feels_like").getAsDouble());
                weather.setUmidita(mainObj.get("humidity").getAsInt());
                weather.setPressione(mainObj.get("pressure").getAsInt());
            }

            JsonObject windObj = getJsonObjectOrNull(json, "wind");
            if (windObj != null) {
                JsonElement speedElement = windObj.get("speed");
                if (speedElement != null && !speedElement.isJsonNull()) {
                    weather.setVelocitaVento(speedElement.getAsDouble());
                }
            }

            JsonElement visibilityElement = json.get("visibility");
            if (visibilityElement != null && !visibilityElement.isJsonNull()) {
                weather.setVisibilita(visibilityElement.getAsInt());
            }

            return weather;
        } catch (Exception e) {
            logger.severe(String.format("Error parsing weather JSON: %s", e.getMessage()));
            return null;
        }
    }

    public String getWeatherIconUrl(String icon) {
        return String.format("https://openweathermap.org/img/wn/%s@2x.png", icon);
    }

    private JsonObject getJsonObjectOrNull(JsonObject obj, String key) {
        JsonElement element = obj.get(key);
        return (element != null && !element.isJsonNull() && element.isJsonObject()) ? element.getAsJsonObject() : null;
    }
}

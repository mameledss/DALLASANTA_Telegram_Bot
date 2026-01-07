package com.flightbot.config;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.util.logging.Logger;

public class ConfigLoader { //pattern Singleton
    private static final Logger logger = Logger.getLogger(ConfigLoader.class.getName());
    private static ConfigLoader instance;
    private Configurations configs = new Configurations(); //per leggere e caricare il file di configurazione
    private Configuration config; //rappresenta la configurazione caricata

    private ConfigLoader() {
        try {
            config = configs.properties("conf.properties"); //carica il file di configurazione
        } catch (ConfigurationException e) {
            logger.severe("File conf.properties non disponibile");
            System.exit(-1);
        }
    }

    public static ConfigLoader getInstance() {
        if (instance == null) {
            instance = new ConfigLoader();
        }
        return instance;
    }

    public String getProperty(String key) {
        return config.getString(key);
    }
    public String getProperty(String key, String defaultValue) {
        return config.getString(key, defaultValue);
    }

    //metodi per velocizzare l'ottenimento dei paramentri
    public static String get(String key) {
        return getInstance().getProperty(key);
    }
    public static String get(String key, String defaultValue) {
        return getInstance().getProperty(key, defaultValue);
    }

    //telegram Bot
    public static String getTelegramBotUsername() {
        return get("telegram.bot.username");
    }
    public static String getTelegramBotToken() {
        return get("telegram.bot.token");
    }

    //aviationStack
    public static String getAviationStackApiKey() {
        return get("aviationstack.api.key");
    }

    //aeroDataBox
    public static String getAeroDataBoxApiKey() {
        return get("aerodatabox.api.key");
    }

    //openWeatherMap
    public static String getOpenWeatherApiKey() {
        return get("openweather.api.key");
    }

    //amadeus
    public static String getAmadeusApiKey() {
        return get("amadeus.api.key");
    }
    public static String getAmadeusApiSecret() {
        return get("amadeus.api.secret");
    }

    //database
    public static String getDatabaseUrl() {
        return get("database.url");
    }
}
package com.flightbot.services;

import com.flightbot.models.Luggage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class LuggageService {

    private static final Logger logger = Logger.getLogger(LuggageService.class.getName());
    private static final String LUGGAGE_URL =
            "https://www.carpisa.it/it-it/pages/dimensioni-e-peso-consentiti";

    public List<Luggage> scrapeLuggageInfo() throws IOException {
        logger.info(String.format("Iniziando lo scraping delle informazioni sui bagagli da: %s", LUGGAGE_URL));
        List<Luggage> luggageInfoList = new ArrayList<>();

        try {
            Document document = Jsoup.connect(LUGGAGE_URL)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get();

            Element table = document.selectFirst("table.ck-table-resized");
            if (table == null) {
                logger.warning("Tabella delle informazioni sui bagagli non trovata");
                return luggageInfoList;
            }

            Elements rows = table.select("tbody tr");

            for (Element row : rows) {
                Elements cells = row.select("td");
                if (cells.size() >= 3) {
                    String airline = cells.get(0).text().trim();
                    String maxDimensions = cells.get(1).text().trim();
                    String maxWeight = cells.get(2).text().trim();

                    if (!airline.isEmpty() && !maxDimensions.isEmpty()) {
                        luggageInfoList.add(
                                new Luggage(airline, maxDimensions, maxWeight)
                        );
                    }
                }
            }
            logger.info(String.format("Scraping completato. Trovate %s compagnie aeree", luggageInfoList.size()));

        } catch (IOException e) {
            logger.severe(String.format("Errore durante lo scraping: %s", e.getMessage()));
            throw e;
        }
        return luggageInfoList;
    }

    /**
     * Restituisce tutte le informazioni sui bagagli
     */
    public List<Luggage> getInfoBagaglio() throws IOException {
        return scrapeLuggageInfo();
    }

    /**
     * Ricerca una compagnia aerea (case-insensitive e parziale)
     */
    public Luggage getLuggageInfoByAirline(String airlineName) throws IOException {
        List<Luggage> allInfo = scrapeLuggageInfo();

        for (Luggage info : allInfo) {
            if (info.getCompagnia().toLowerCase()
                    .contains(airlineName.toLowerCase())) {
                return info;
            }
        }
        return null;
    }

    /**
     * Restituisce tutte le compagnie aeree disponibili
     */
    public List<String> getAvailableAirlines() throws IOException {
        List<Luggage> allInfo = scrapeLuggageInfo();
        List<String> airlines = new ArrayList<>();

        for (Luggage info : allInfo) {
            airlines.add(info.getCompagnia());
        }

        return airlines;
    }
}

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
    private static final String LUGGAGE_URL = "https://www.carpisa.it/it-it/pages/dimensioni-e-peso-consentiti";

    public List<Luggage> scrapeInfoBagaglio() throws IOException {
        logger.info(String.format("Iniziando lo scraping delle informazioni sui bagagli da: %s", LUGGAGE_URL));
        List<Luggage> listaInfoBagagli = new ArrayList<>();

        try { //Jsoup si connette all'URL simulando un browser (tramite userAgent), imposta timeout di 10 secondi e scarica documento HTML
            Document document = Jsoup.connect(LUGGAGE_URL)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get();

            Element tabella = document.selectFirst("table.ck-table-resized"); //cerca prima tabella HTML con classe CSS ck-table-resized
            if (tabella == null) {
                logger.warning("Tabella delle informazioni sui bagagli non trovata");
                return listaInfoBagagli;
            }

            Elements righe = tabella.select("tbody tr"); //seleziona tutte le righe tr all'interno del corpo della tabella tbody

            for (Element riga : righe) { //per ogni riga
                Elements celle = riga.select("td"); //estrae tutte le celle
                if (celle.size() >= 3) { //verifica che ce ne siano almeno 3 (compagnia, dimensioni, peso)
                    String compagnia = celle.get(0).text().trim(); //prima cella->compagnia
                    String maxDimensioni = celle.get(1).text().trim(); //seconda->dimensioni
                    String maxPeso = celle.get(2).text().trim(); //terza->peso

                    if (!compagnia.isEmpty() && !maxDimensioni.isEmpty())
                        listaInfoBagagli.add(new Luggage(compagnia, maxDimensioni, maxPeso));
                }
            }
            logger.info(String.format("Scraping completato. Trovate %s compagnie aeree", listaInfoBagagli.size()));

        } catch (IOException e) {
            logger.severe(String.format("Errore durante lo scraping: %s", e.getMessage()));
            throw e;
        }
        return listaInfoBagagli;
    }

    public List<Luggage> getInfoBagaglio() throws IOException {
        return scrapeInfoBagaglio();
    }
}
package com.flightbot.services;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
Usa Tesseract per eseguire il riconoscimento OCR (Optical Character Recognition) su immagini.
Nella cartella src/main/resources/tessdata sono presenti i dati .traineddata di Tesseract per l'inglese.
*/
public class OcrService {
    private final Tesseract tesseract;
    
    /*pattern per riconoscere codici di volo (es. BA123, LH456, etc)
    - \\b -> indica confine di parola, per evitare che trovi numeri all'interno di codici più lunghi
    - [A-Z]{2} -> da A a Z, due lettere
    - \\d{1,4} -> da 1 a 4 cifre
    */
    private static final Pattern FLIGHT_CODE_PATTERN = Pattern.compile(
            "\\b([A-Z]{2}\\d{1,4})\\b",
            Pattern.CASE_INSENSITIVE
    );
    
    /*pattern per riconoscere orari (HH:MM in vari formati)
    - \\b -> indica confine di parola
    - [0-1][0-9] -> numero da 0 a 19
    - 2[0-3] -> da 00 a 23
    - ([:|\s])? -> separatore ":" o " ", ? -> opzionale
    - [0-5][0-9] -> numero da 00 a 59
    */
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "\\b([0-1][0-9]|2[0-3])([:|\\s])?([0-5][0-9])\\b"
    );
    
    public OcrService() {
        this.tesseract = new Tesseract();
        this.tesseract.setDatapath("src/main/resources/tessdata"); //imposta percorso ai dati di Tesseract
        this.tesseract.setLanguage("eng"); //lingua inglese per i tabelloni
    }

    private List<Map<String, String>> analizzaTabellone(String testo) {
        List<Map<String, String>> voli = new ArrayList<>();

        String[] linee = testo.split("\n"); //divide il testo in righe
        
        for (String linea : linee) { //per ogni linea
            if (linea.trim().isEmpty()) //salta le righe vuote
                continue;
            //System.out.println(linea);
            Map<String, String> infoVolo = estraiVoloDaLinea(linea);
            if (!infoVolo.isEmpty())
                voli.add(infoVolo);
        }
        return voli;
    }

    private Map<String, String> estraiVoloDaLinea(String linea) {
        Map<String, String> infoVolo = new HashMap<>();

        Matcher matcherVolo = FLIGHT_CODE_PATTERN.matcher(linea); //verifica se linea contiene un codice
        if (matcherVolo.find())
            infoVolo.put("flightCode", matcherVolo.group(1).toUpperCase()); //group(1) estrae la prima sottoespressione
        else
            return infoVolo; //nessun codice volo trovato

        Matcher matcherTempo = TIME_PATTERN.matcher(linea); //verifica se linea contiene orario
        if (matcherTempo.find()) {
            String orario = matcherTempo.group().replace(" ", ":"); //rimpiazza spazi con ":"
            orario = formattaOrario(orario); //garantisce il formato HH:MM
            infoVolo.put("time", orario);
        }
        return infoVolo;
    }

    private String formattaOrario(String orario) {
        if (orario == null || orario.isEmpty())
            return orario;

        String soloNumeri = orario.replaceAll("[^0-9]", ""); //rimuove tutti i caratteri non numerici

        if (soloNumeri.length() == 4) //se ha 4 cifre
            return soloNumeri.substring(0, 2) + ":" + soloNumeri.substring(2); //inserisce i due punti nel mezzo
        
        //se ha meno di 4 cifre o ha già il formato corretto, restituisce come è
        return orario.contains(":") ? orario : soloNumeri;
    }

    public Map<String, String> estraiVoliEOre(String percorsoImm) {
        Map<String, String> voloMap = new LinkedHashMap<>();
        
        try {
            String testoEstratto = tesseract.doOCR(new File(percorsoImm));
            List<Map<String, String>> voli = analizzaTabellone(testoEstratto);
            
            for (Map<String, String> volo : voli) { //per ogni volo
                String codice = volo.get("flightCode");
                String ora = volo.get("time");
                if (codice != null && ora != null)
                    voloMap.put(codice, ora);
            }
        } catch (TesseractException e) {
            System.err.println("Errore durante l'OCR: " + e.getMessage());
        }
        return voloMap;
    }
}
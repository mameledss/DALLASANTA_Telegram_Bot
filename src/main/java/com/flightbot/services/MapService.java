package com.flightbot.services;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class MapService {
    private static final Logger logger = Logger.getLogger(MapService.class.getName());
    private static final String URL_TESSERE = "https://tile.openstreetmap.org";
    private static final int DIM_TESSERA = 256; //OpenStreetMap divide il mondo in tessere quadrate di 256x256 pixel
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS) //tempo di attesa massimo di 10 secondi
            .readTimeout(30, TimeUnit.SECONDS) //30 secondi per scaricare i dati
            .build();

    public File generaMappaVolo(double daLat, double daLon, double aLat, double aLon) {
        try {
            int zoom = calcolaZoom(daLat, daLon, aLat, aLon, 800, 600); //calcola il livello di zoom ottimale
            //converte coordinate di partenza e arrivo a coordinate delle rispettive tessere, array contiene [x, y] della tessera
            int[] daTessera = latLonATessera(daLat, daLon, zoom);
            int[] aTessera = latLonATessera(aLat, aLon, zoom);

            //trova il rettangolo minimo che contiene entrambi i punti
            int minTesseraX = Math.min(daTessera[0], aTessera[0]) - 1; //-1 aggiunge una tessera di margine a sinistra/alto
            int maxTesseraX = Math.max(daTessera[0], aTessera[0]) + 1; //+1 aggiunge una tessera di margine a destra/basso
            int minTesseraY = Math.min(daTessera[1], aTessera[1]) - 1;
            int maxTesseraY = Math.max(daTessera[1], aTessera[1]) + 1;

            int tessereX = maxTesseraX - minTesseraX + 1; //quante tessere in larghezza
            int tessereY = maxTesseraY - minTesseraY + 1; //quante tessere in altezza
            int largMappa = tessereX * DIM_TESSERA; //larghezza mappa in pixel
            int altMappa = tessereY * DIM_TESSERA; //altezza in pixel

            BufferedImage mappa = new BufferedImage(largMappa, altMappa, BufferedImage.TYPE_INT_RGB); //crea immagine vuota delle dimensioni calcolate
            Graphics2D g = mappa.createGraphics(); //ottiene oggetto Graphics2D per disegnarci sopra

            int tessereScaricate = 0;
            for (int x = minTesseraX; x <= maxTesseraX; x++) { //cicla su tutte le tessere
                for (int y = minTesseraY; y <= maxTesseraY; y++) {
                    BufferedImage tessera = scaricaTessera(x, y, zoom); //scarica la tessera
                    if (tessera != null) {
                        int Xdisegno = (x - minTesseraX) * DIM_TESSERA; //calcola posizione orizzontale (in pixel) della tessera nell'immagine
                        int Ydisegno = (y - minTesseraY) * DIM_TESSERA; //e quella verticale
                        g.drawImage(tessera, Xdisegno, Ydisegno, null); //disegna la tessera
                        tessereScaricate++;
                    } else { //se fallisce il download
                        int drawX = (x - minTesseraX) * DIM_TESSERA; //fa lo stesso di prima
                        int drawY = (y - minTesseraY) * DIM_TESSERA;
                        g.setColor(new Color(200, 220, 255)); //azzurro
                        //disegna un placeholder al posto della tessera
                        g.fillRect(drawX, drawY, DIM_TESSERA, DIM_TESSERA);
                        g.setColor(Color.GRAY);
                        g.drawRect(drawX, drawY, DIM_TESSERA, DIM_TESSERA);
                    }
                }
            }
            logger.info(String.format("Scaricate %s di %s tessere per la mappa", tessereScaricate, tessereX * tessereY));

            //converte le coordinate a coordinate pixel sulla mappa
            int x1 = lonAPixel(daLon, minTesseraX, zoom);
            int y1 = latAPixel(daLat, minTesseraY, zoom);
            int x2 = lonAPixel(aLon, minTesseraX, zoom);
            int y2 = latAPixel(aLat, minTesseraY, zoom);

            //assicura che i punti non finiscano troppo vicino ai bordi (almeno 20 pixel di margine)
            x1 = Math.max(20, Math.min(largMappa - 20, x1));
            y1 = Math.max(20, Math.min(altMappa - 20, y1));
            x2 = Math.max(20, Math.min(largMappa - 20, x2));
            y2 = Math.max(20, Math.min(altMappa - 20, y2));

            logger.info(String.format("Coordinate di volo: (%s, %s) -> (%s, %s) pixels: (%s, %s) -> (%s, %s) on %sx%s map",
                    daLat, daLon, aLat, aLon, x1, y1, x2, y2, largMappa, altMappa));

            //disegna la linea della rotta route line
            g.setColor(new Color(255, 0, 0, 255)); //rosso
            g.setStroke(new BasicStroke(6)); //linea spessa
            g.drawLine(x1, y1, x2, y2);

            //disegna una seconda linea verde per maggiore contrasto
            g.setColor(new Color(0, 255, 0, 255)); //verde
            g.setStroke(new BasicStroke(2));
            g.drawLine(x1, y1, x2, y2);

            //disegna cerchi aeroporto
            g.setColor(new Color(0, 0, 255, 255)); //blu
            g.fillOval(x1 - 15, y1 - 15, 30, 30); //-15 per centrarlo sulla coordinata
            g.fillOval(x2 - 15, y2 - 15, 30, 30);

            //disegna i bordi dell'aeroporto
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(3));
            g.drawOval(x1 - 15, y1 - 15, 30, 30);
            g.drawOval(x2 - 15, y2 - 15, 30, 30);

            //disegna le etichette
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 16));
            FontMetrics fm = g.getFontMetrics();

            //etichetta per la partenza
            String etichettaPartenza = "Partenza";
            int partenLarg = fm.stringWidth(etichettaPartenza); //calcola larghezza testo
            g.setColor(new Color(255, 255, 255, 180));
            //centra box orizzontalmente, posiziona box sopra aeroporto, aggiunge padding laterale di 5px per lato, altezza box
            g.fillRect(x1 - partenLarg/2 - 5, y1 - 35, partenLarg + 10, 20);
            g.setColor(Color.BLACK);
            g.drawRect(x1 - partenLarg/2 - 5, y1 - 35, partenLarg + 10, 20);
            g.drawString(etichettaPartenza, x1 - partenLarg/2, y1 - 20); //scrive "Partenza" dentro il box

            //stessa cosa per etichetta arrivo
            String arrLabel = "Arrivo";
            int arrWidth = fm.stringWidth(arrLabel);
            g.setColor(new Color(255, 255, 255, 180));
            g.fillRect(x2 - arrWidth/2 - 5, y2 - 35, arrWidth + 10, 20);
            g.setColor(Color.BLACK);
            g.drawRect(x2 - arrWidth/2 - 5, y2 - 35, arrWidth + 10, 20);
            g.drawString(arrLabel, x2 - arrWidth/2, y2 - 20);

            g.dispose(); //libera risorse

            //salva in un file temporaneo
            File tempFile = File.createTempFile("flight_map_", ".png");
            ImageIO.write(mappa, "PNG", tempFile);
            tempFile.deleteOnExit(); //file cancellato al termine del programma

            return tempFile;
        } catch (Exception e) {
            logger.severe(String.format("Errore nella generazione mappa: %s", e.getMessage()));
            return null;
        }
    }

    public File generaMappaLive(double lat, double lon, String numeroVolo, int altitudine, int velocita) {
        try {
            int zoom = 10; //usa uno zoom fisso di 10
            int[] tesseraCentrale = latLonATessera(lat, lon, zoom); //ottiene le coordinate della tessera per la posizione dell'aereo

            //ottiene le tessere laterali (3x3)
            int minTesseraX = tesseraCentrale[0] - 1;
            int maxTesseraX = tesseraCentrale[0] + 1;
            int minTesseraY = tesseraCentrale[1] - 1;
            int maxTesseraY = tesseraCentrale[1] + 1;

            //calcola le dimensioni della mappa
            int tessereX = maxTesseraX - minTesseraX + 1; //quante tessere in larghezza
            int tessereY = maxTesseraY - minTesseraY + 1; //quante in altezza
            int largMappa = tessereX * DIM_TESSERA;
            int altMappa = tessereY * DIM_TESSERA;

            BufferedImage mappa = new BufferedImage(largMappa, altMappa, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = mappa.createGraphics();

            for (int x = minTesseraX; x <= maxTesseraX; x++) { //per ogni tessera
                for (int y = minTesseraY; y <= maxTesseraY; y++) {
                    BufferedImage tessera = scaricaTessera(x, y, zoom); //la scarica
                    if (tessera != null) {
                        int disegnoX = (x - minTesseraX) * DIM_TESSERA;
                        int disegnoY = (y - minTesseraY) * DIM_TESSERA;
                        g.drawImage(tessera, disegnoX, disegnoY, null);
                    }
                }
            }

            //converte la posizione dell'aereo a coordinate pixel sulla mappa
            int aereoX = lonAPixel(lon, minTesseraX, zoom);
            int aereoY = latAPixel(lat, minTesseraY, zoom);

            //assicura che i punti non finiscano troppo vicino ai bordi (almeno 20 pixel di margine)
            aereoX = Math.max(20, Math.min(largMappa - 20, aereoX));
            aereoY = Math.max(20, Math.min(altMappa - 20, aereoY));

            logger.info(String.format("Posizione aereo sulla mappa: (%s, %s) on %sx%s map", aereoX, aereoY, largMappa, altMappa));

            //disegna l'aereo
            g.setColor(new Color(255, 0, 0, 255)); //rosso
            //crea gli array per i punti della freccia che rappresenta l'aereo
            int[] xPunti = new int[4];
            int[] yPunti = new int[4];

            //assegna valori per coordinate orizzontali
            xPunti[0] = aereoX; //punta
            xPunti[1] = aereoX - 25; //ala sinistra
            xPunti[2] = aereoX; //rientro coda
            xPunti[3] = aereoX + 25; //ala destra

            //assegna valori per coordinate verticali
            yPunti[0] = aereoY - 30; //punta
            yPunti[1] = aereoY + 20; //ala sinistra
            yPunti[2] = aereoY; //rientro coda
            yPunti[3] = aereoY + 20; //ala destra
            g.fillPolygon(xPunti, yPunti, 4);

            //disegna il bordo
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(3));
            g.drawPolygon(xPunti, yPunti, 4);

            //disegna il box per le informazioni
            g.setColor(new Color(255, 255, 255, 230));
            g.fillRoundRect(10, 10, 320, 130, 15, 15);
            g.setColor(new Color(0, 0, 0, 200));
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(10, 10, 320, 130, 15, 15);

            //testo info
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.drawString("Volo: " + numeroVolo, 20, 38);
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            g.drawString(String.format("Posizione: %.4f°, %.4f°", lat, lon), 20, 65);
            g.drawString(String.format("Altitudine: %d m", altitudine), 20, 90);
            g.drawString(String.format("Velocità: %d km/h", velocita), 20, 115);

            g.dispose();

            File tempFile = File.createTempFile("live_tracking_", ".png");
            ImageIO.write(mappa, "PNG", tempFile);
            tempFile.deleteOnExit();

            return tempFile;
        } catch (Exception e) {
            logger.severe(String.format("Errore generazione mappa live: %s", e.getMessage()));
            return null;
        }
    }
    //metodo di ChatGPT
    private int calcolaZoom(double lat1, double lon1, double lat2, double lon2, int maxWidth, int maxHeight) {
        //calcola differenza tra latitudine e longitudine in radianti
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        //calcola valore intermedio a che rappresenta quadrato della metà della corda tra due punti sulla sfera terrestre
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double distanza = 6371 * c; //raggio terra in km

        //stima il livello di zoom in base alla distanza
        //approssimazione: zoom 8 copre 1500km, ogni livello di zoom dimezza la distanza
        int zoom = 8;
        double copertura = 1500;
        while (copertura > distanza * 1.5 && zoom < 15) {
            zoom++;
            copertura /= 2;
        }
        while (copertura < distanza * 0.8 && zoom > 3) {
            zoom--;
            copertura *= 2;
        }
        return Math.max(3, Math.min(15, zoom));
    }
    //metodo di ChatGPT
    private int[] latLonATessera(double lat, double lon, int zoom) {
        double latRad = Math.toRadians(lat);
        int n = 1 << zoom;
        int x = (int) Math.floor((lon + 180.0) / 360.0 * n);
        int y = (int) Math.floor((1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * n);
        return new int[]{x, y};
    }
    //metodo di ChatGPT
    private int lonAPixel(double lon, int tileX, int zoom) {
        int n = 1 << zoom;
        double x = (lon + 180.0) / 360.0 * n * DIM_TESSERA;
        return (int) Math.round(x - tileX * DIM_TESSERA);
    }
    //metodo di ChatGPT
    private int latAPixel(double lat, int tileY, int zoom) {
        int n = 1 << zoom;
        double latRad = Math.toRadians(lat);
        double y = (1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * n * DIM_TESSERA;
        return (int) Math.round(y - tileY * DIM_TESSERA);
    }

    private BufferedImage scaricaTessera(int x, int y, int zoom) {
        String url = String.format("%s/%d/%d/%d.png", URL_TESSERE, zoom, x, y);
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "FlightBot/1.0")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    BufferedImage tessera = ImageIO.read(response.body().byteStream());
                    if (tessera != null)
                        return tessera;
                    else {
                        logger.severe(String.format("Fallita codifica immagine tessera: %s/%s/%s", zoom, x, y));
                    }
                } else
                    logger.severe(String.format("Fallito download tessera %s/%s/%s: %s", zoom, x, y, response.code()));
            }
        } catch (Exception e) {
            logger.severe(String.format("Errore download tessera %s/%s/%s: %s", zoom, x, y, e.getMessage()));
        }
        return null;
    }
}
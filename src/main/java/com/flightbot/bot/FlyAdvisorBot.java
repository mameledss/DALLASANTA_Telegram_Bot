package com.flightbot.bot;

import com.flightbot.config.ConfigLoader;
import com.flightbot.database.DatabaseManager;
import com.flightbot.models.Airport;
import com.flightbot.models.Flight;
import com.flightbot.models.Luggage;
import com.flightbot.models.Ticket;
import com.flightbot.models.Weather;
import com.flightbot.services.*;
import org.quartz.SchedulerException;
import java.util.logging.Logger;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlyAdvisorBot implements LongPollingSingleThreadUpdateConsumer {
    //per i log uso Logger di java.util.logging
    private static final Logger logger = Logger.getLogger(FlyAdvisorBot.class.getName());
    private final TelegramClient telegramClient; //client per inviare e ricevere messaggi
    //vari servizi del bot
    private final FlightService flightService;
    private final AirportService airportService;
    private final WeatherService weatherService;
    private final AmadeusService amadeusService;
    private final MapService mapService;
    private final ImageService imageService;
    private final NotificationScheduler notificationScheduler;
    private final LuggageService luggageService;
    //map per tenere traccia di stato conversazione per ogni utente
    private final Map<Long, String> userStates = new HashMap<>(); //<id utente, stato corrente>
    //memorizza i dati raccolti durante la conversazione
    private final Map<Long, Map<String, String>> userContexts = new HashMap<>(); //<id utente, map di dati raccolti>

    public FlyAdvisorBot() throws SchedulerException {
        //inizializza il client Telegram ottenendo il token dal file di configurazione
        this.telegramClient = new OkHttpTelegramClient(ConfigLoader.getTelegramBotToken());
        //inizializza i servizi del bot
        this.flightService = new FlightService();
        this.airportService = new AirportService();
        this.weatherService = new WeatherService();
        this.amadeusService = new AmadeusService();
        this.mapService = new MapService();
        this.imageService = new ImageService();
        this.notificationScheduler = new NotificationScheduler();
        this.luggageService = new LuggageService();

        DatabaseManager.getInstance(); //inizializza il database
    }

    @Override
    public void consume(Update update) {
        try {
            if (update.hasMessage()) { //se l'update contiene un messaggio
                Message msg = update.getMessage(); //estrae il messaggio
                long chatId = msg.getChatId(); //estrae l'id del chat

                if (msg.hasText())  //se il messaggio contiene testo
                    gestisciMessaggioTesto(chatId, msg.getText()); //chiama il metodo per gestire il testo
            } else if (update.hasCallbackQuery()) { //gestione click pulsante inline nella chat
                gestisciCallbackQuery(update);
            }
        } catch (Exception e) {
            logger.severe(String.format("Errore nel processare l'update: %s", e.getMessage()));
        }
    }

    private void gestisciMessaggioTesto(long chatId, String testo) {
        String stato = userStates.get(chatId); //estrae stato della conversazione in base all'id utente
        if (testo.startsWith("/")) //se il testo inizia con /
            gestisciComando(chatId, testo); //chiama metodo per gestione dei comandi
        else if (stato != null) //se lo stato non è null
            gestisciStato(chatId, testo, stato); //chiama metodo per gestire lo stato
        else //altrimenti
            invioMsg(chatId, "Usa /help per vedere i comandi disponibili."); //comando non valido
    }

    private void gestisciComando(long chatId, String comando) {
        String[] parti = comando.split(" ", 2); //separa la stringa in 2
        String cmd = parti[0].toLowerCase(); //la prima parte in minuscolo
        String args = parti.length > 1 ? parti[1] : ""; //se ci sono almeno due parti, args è la seconda parte, altrimenti ""
        //switch comandi possibili
        switch (cmd) {
            case "/start":
                invioMsgBenvenuto(chatId); //invia messaggio di benvenuto
                break;
            case "/help":
                invioMsgAiuto(chatId); //invia messaggio di help
                break;
            case "/track":
                if (!args.isEmpty()) //se la seconda parte non è vuota
                    tracciaVolo(chatId, args); //chiama metodo per tracciare volo
                else {
                    invioMsg(chatId, "Inserisci il numero del volo da tracciare:");
                    userStates.put(chatId, "AWAITING_FLIGHT_NUMBER"); //imposta lo stato di attesa per numero volo
                }
                break;
            case "/flight":
                if (!args.isEmpty()) //se la seconda parte non è vuota
                    getInfoVolo(chatId, args); //chiama metodo per info volo
                else {
                    invioMsg(chatId, "Inserisci il numero del volo:");
                    userStates.put(chatId, "AWAITING_FLIGHT_INFO"); //imposta stato di attesa numero volo
                }
                break;
            case "/airport":
                if (!args.isEmpty())
                    getInfoAeroporto(chatId, args);
                else {
                    invioMsg(chatId, "Inserisci il codice IATA dell'aeroporto (es. MXP):");
                    userStates.put(chatId, "AWAITING_AIRPORT_CODE");
                }
                break;
            case "/weather":
                if (!args.isEmpty())
                    getInfoMeteo(chatId, args);
                else {
                    invioMsg(chatId, "Inserisci il nome della città:");
                    userStates.put(chatId, "AWAITING_WEATHER_CITY");
                }
                break;
            case "/tickets":
                invioMsg(chatId, "Inserisci partenza (codice IATA):");
                userStates.put(chatId, "AWAITING_TICKET_FROM");
                userContexts.put(chatId, new HashMap<>());
                break;
            case "/notify":
                invioMsg(chatId, "Inserisci il numero del volo per impostare una notifica:");
                userStates.put(chatId, "AWAITING_NOTIFY_FLIGHT");
                userContexts.put(chatId, new HashMap<>());
                break;
            case "/myflights":
                mostraVoliTracciati(chatId);
                break;
            case "/menu":
                mostraMenu(chatId);
                break;
            case "/luggage":
                mostraCompagnieAeree(chatId);
                break;
            case "/settings":
            case "/preferences":
                mostraImpostazioni(chatId);
                break;
            default:
                invioMsg(chatId, "Comando non riconosciuto. Usa /help per la lista dei comandi.");
        }
    }

    private void gestisciStato(long chatId, String input, String stato) {
        Map<String, String> contesto = userContexts.get(chatId); //prende dalla map userContexts la sotto-mappa associata all'utente

        switch (stato) {
            case "AWAITING_FLIGHT_NUMBER":
                tracciaVolo(chatId, input);
                userStates.remove(chatId); //rimuove lo stato corrente della conversazione
                break;
            case "AWAITING_FLIGHT_INFO":
                getInfoVolo(chatId, input);
                userStates.remove(chatId);
                break;
            case "AWAITING_AIRPORT_CODE":
                getInfoAeroporto(chatId, input.toUpperCase());
                userStates.remove(chatId);
                break;
            case "AWAITING_WEATHER_CITY":
                getInfoMeteo(chatId, input);
                userStates.remove(chatId);
                break;
            case "AWAITING_TICKET_FROM":
                contesto.put("from", input.toUpperCase());
                invioMsg(chatId, "Inserisci destinazione (codice IATA):");
                userStates.put(chatId, "AWAITING_TICKET_TO");
                break;
            case "AWAITING_TICKET_TO":
                contesto.put("to", input.toUpperCase());
                invioMsg(chatId, "Inserisci la data (YYYY-MM-DD):");
                userStates.put(chatId, "AWAITING_TICKET_DATE");
                break;
            case "AWAITING_TICKET_DATE":
                cercaBiglietti(chatId, contesto.get("from"), contesto.get("to"), input);
                userStates.remove(chatId);
                userContexts.remove(chatId);
                break;
            case "AWAITING_NOTIFY_FLIGHT":
                contesto.put("flight", input.toUpperCase());
                invioMsg(chatId, "Tra quanti minuti vuoi ricevere la notifica?");
                userStates.put(chatId, "AWAITING_NOTIFY_TIME");
                break;
            case "AWAITING_NOTIFY_TIME":
                try {
                    int minuti = Integer.parseInt(input);
                    gestisciNotifiche(chatId, contesto.get("flight"), minuti);
                } catch (NumberFormatException e) {
                    invioMsg(chatId, "Inserisci un numero valido di minuti.");
                }
                userStates.remove(chatId);
                userContexts.remove(chatId);
                break;
            case "AWAITING_NOTIFICATION_INTERVAL":
                try {
                    int minuti = Integer.parseInt(input.trim()); //converte a intero
                    if (minuti < 1 || minuti > 1440) { // max 24 ore
                        invioMsg(chatId, "Inserisci un valore tra 1 e 1440 minuti (24 ore).");
                        return;
                    }
                    try { //modifica intervallo notifiche se le notifiche sono attivate
                        DatabaseManager.getInstance().setPreferenzeUtente(chatId, DatabaseManager.getInstance().areNotificheAbilitate(chatId), minuti);
                    } catch (SQLException e) {
                        logger.severe(String.format("Errore nella modifica intervallo notifiche: %s", e.getMessage()));
                    }
                    invioMsg(chatId, "✅ Intervallo notifiche aggiornato a " + minuti + " minuti.");
                    mostraImpostazioni(chatId);
                } catch (NumberFormatException e) {
                    invioMsg(chatId, "Inserisci un numero valido di minuti.");
                }
                userStates.remove(chatId); //rimuove lo stato corrente della conversazione
                break;
        }
    }

    private void gestisciCallbackQuery(Update update) {
        String datiPulsante = update.getCallbackQuery().getData(); //estrae i dati associati al bottone cliccato
        long chatId = update.getCallbackQuery().getMessage().getChatId(); //ottiene l'ID chat da cui proviene il click

        String[] parti = datiPulsante.split(":"); //separa i dati con separatore ":"
        String azione = parti[0]; //l'azione è la prima parte

        switch (azione) {
            case "menu":
                if (parti.length > 1) {
                    String azioneMenu = parti[1];
                    gestisciMenu(chatId, azioneMenu);
                }
                break;
            case "view_map": //todo serve?
                if (parti.length > 1) {
                    String flightNumber = parti[1];
                    sendFlightMap(chatId, flightNumber);
                }
                break;
            case "view_aircraft":
                if (parti.length > 1) {
                    String aereoId = parti[1]; //l'id aereo è la seconda parte
                    inviaFotoAereo(chatId, aereoId);
                }
                break;
            case "track_flight":
                if (parti.length > 1) {
                    String numeroVolo = parti[1];
                    tracciaVolo(chatId, numeroVolo);
                }
                break;
            case "luggage":
                if (parti.length > 1) {
                    String compagnia = parti[1];
                    mostraInfoBagaglio(chatId, compagnia);
                }
                break;
            case "settings":
                if (parti.length > 1) {
                    String azioneImpostazioni = parti[1];
                    gestisciImpostazioni(chatId, parti);
                }
                break;
        }
    }

    private void gestisciMenu(long chatId, String azioneMenu) {
        switch (azioneMenu) {
            case "flight":
                gestisciComando(chatId, "/flight");
                break;
            case "airport":
                gestisciComando(chatId, "/airport");
                break;
            case "weather":
                gestisciComando(chatId, "/weather");
                break;
            case "tickets":
                gestisciComando(chatId, "/tickets");
                break;
            case "luggage":
                gestisciComando(chatId, "/luggage");
                break;
            case "settings":
                mostraImpostazioni(chatId);
                break;
        }
    }
    //parti[0]=settings, parti[1]=notifications|interval|main, parti[2]=enable|disable
    private void gestisciImpostazioni(long chatId, String[] parti) {
        try {
            if (parti.length < 2) return; //se ci sono meno di due parti

            String azione = parti[1]; //azione è a parti[1]

            switch (azione) {
                case "notifications":
                    if (parti.length > 2) {
                        String valore = parti[2]; //enable o disable
                        boolean abilitato = "enable".equals(valore); //se il valore é uguale a "enable"
                        DatabaseManager.getInstance().setPreferenzeUtente(chatId, abilitato, DatabaseManager.getInstance().getIntervalloMinuti(chatId));
                        invioMsg(chatId, "✅ Notifiche " + (abilitato ? "attivate" : "disattivate"));
                        mostraImpostazioni(chatId);
                    }
                    break;
                case "interval":
                    invioMsg(chatId, "⏰ Inserisci l'intervallo desiderato in minuti (es. 15, 30, 60):");
                    userStates.put(chatId, "AWAITING_NOTIFICATION_INTERVAL"); //stato attesa intervallo
                    break;
                case "main":
                    mostraMenu(chatId); //ritorno al menu
                    break;
            }
        } catch (Exception e) {
            logger.severe(String.format("Errore nella gestione impostazioni: %s", e.getMessage()));
            invioMsg(chatId, "❌ Errore nell'aggiornamento delle impostazioni.");
        }
    }

    private void invioMsgBenvenuto(long chatId) {
        String benvenuto = """
                ✈️ Benvenuto in FlyAdvisor! ✈️
                
                Il tuo assistente personale per i voli!
                
                Funzionalità disponibili:
                • 📍 Tracking voli in tempo reale
                • 🗺️ Mappe dinamiche
                • 📊 Statistiche e grafici
                • 🏢 Info aeroporti
                • 🌤️ Meteo
                • 🎫 Ricerca biglietti
                • 🔔 Notifiche personalizzate
                • 🧳 Info bagaglio a mano
                
                Usa /help per vedere tutti i comandi!
                """;
        inviaMessaggioBottoni(chatId, benvenuto);
    }

    private void invioMsgAiuto(long chatId) {
        String help = """
                📋 Comandi disponibili:
                
                /flight <numero> - Info su un volo
                /track <numero> - Traccia un volo
                /airport <codice> - Info aeroporto
                /weather <città> - Meteo
                /tickets - Cerca biglietti
                /notify - Imposta notifica
                /myflights - I tuoi voli tracciati
                /settings - Impostazioni personali
                /luggage - Info bagaglio a mano
                /menu - Menu principale
                
                Esempi:
                /flight AZ123
                /airport MXP
                /weather Milano
                """;
        invioMsg(chatId, help);
    }

    private void mostraMenu(long chatId) {
        SendMessage msg = new SendMessage(String.valueOf(chatId), "Seleziona un'opzione:");

        List<InlineKeyboardRow> keyboard = new ArrayList<>(); //array di bottoni
        keyboard.add(new InlineKeyboardRow(creaBottone("✈️ Info Volo", "menu:flight")));
        keyboard.add(new InlineKeyboardRow(creaBottone("🏢 Info Aeroporto", "menu:airport")));
        keyboard.add(new InlineKeyboardRow(creaBottone("🌤️ Meteo", "menu:weather")));
        keyboard.add(new InlineKeyboardRow(creaBottone("🎫 Biglietti", "menu:tickets")));
        keyboard.add(new InlineKeyboardRow(creaBottone("🧳 Bagaglio a mano", "menu:luggage")));
        keyboard.add(new InlineKeyboardRow(creaBottone("⚙️ Impostazioni", "menu:settings")));
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(keyboard);
        msg.setReplyMarkup(markup); //invia i pulsanti insieme al messaggio

        try {
            telegramClient.execute(msg);
        } catch (TelegramApiException e) {
            logger.severe(String.format("Errore nell'invio menu: %s", e.getMessage()));
        }
    }

    private void mostraImpostazioni(long chatId) {
        try {
            boolean notificheAbilitate = DatabaseManager.getInstance().areNotificheAbilitate(chatId);
            int intervallo = DatabaseManager.getInstance().getIntervalloMinuti(chatId);

            String stato = """
                ⚙️ Impostazioni attuali:

                🔔 Notifiche: %s
                ⏰ Intervallo notifiche: %d minuti

                Scegli cosa modificare:
                """.formatted(notificheAbilitate ? "Attivate ✅" : "Disattivate ❌", intervallo);

            SendMessage msg = new SendMessage(String.valueOf(chatId), stato);

            List<InlineKeyboardRow> keyboard = new ArrayList<>();
            //formato "settings:notifications:enable" o "settings:notifications:disable"
            keyboard.add(new InlineKeyboardRow(
                    creaBottone(
                            "🔔 " + (notificheAbilitate ? "Disattiva" : "Attiva") + " Notifiche",
                            "settings:notifications:" + (notificheAbilitate ? "disable" : "enable")
                    )
            ));
            keyboard.add(new InlineKeyboardRow(creaBottone("⏰ Cambia Intervallo", "settings:interval")));
            keyboard.add(new InlineKeyboardRow(creaBottone("🔙 Menu", "settings:main")));

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup(keyboard);
            msg.setReplyMarkup(markup);

            telegramClient.execute(msg);
        } catch (Exception e) {
            logger.severe(String.format("Errore nel mostrare impostazioni: %s", e.getMessage()));
            invioMsg(chatId, "❌ Errore nel caricamento delle impostazioni.");
        }
    }
    
    private void mostraCompagnieAeree(long chatId) {
        List<Luggage> listaBagagli = null;
        try {
            listaBagagli = luggageService.getInfoBagaglio(); //ottiene le info sul bagaglio
        } catch (IOException e) {
            logger.severe(String.format("Errore nella lista bagagli: %s", e.getMessage()));
        }
        if (listaBagagli.isEmpty()) { //se la lista è vuota
            invioMsg(chatId, "❌ Impossibile recuperare le informazioni sul bagaglio. Riprova più tardi.");
            return;
        }

        SendMessage msg = new SendMessage(String.valueOf(chatId), "Seleziona una compagnia aerea per vedere le dimensioni e il peso consentiti per il bagaglio a mano:");
        //crea una tastiera con pulsanti per le varie compagnie aeree
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        InlineKeyboardRow rigaCorrente = new InlineKeyboardRow();
        int bottoniPerRiga = 2; //2 bottoni per riga
        int count = 0;

        for (Luggage info : listaBagagli) { //per ogni bagaglio della lista
            //aggiunge alla riga corrente un bottone per la compagnia aerea
            rigaCorrente.add(creaBottone(info.getCompagnia(), "luggage:" + info.getCompagnia()));
            count++;
            if (count % bottoniPerRiga == 0) { //se il numero di bottoni per riga è giusto
                keyboard.add(rigaCorrente); //aggiunge alla tastiera la riga corrente
                rigaCorrente = new InlineKeyboardRow(); //la riga corrente diventa una nuova riga
            }
        }
        if (!rigaCorrente.isEmpty()) //se avanzano bottoni (numero dispari di compagnie)
            keyboard.add(rigaCorrente); //li aggiunge

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(keyboard);
        msg.setReplyMarkup(markup); //invia i bottoni insieme al messaggio

        try {
            telegramClient.execute(msg);
        } catch (TelegramApiException e) {
            logger.severe(String.format("Errore nell'invio delle compagnie aeree: %s", e.getMessage()));
        }
    }

    private void mostraInfoBagaglio(long chatId, String compagnia) {
        List<Luggage> listaBagagli = null;
        try {
            listaBagagli = luggageService.getInfoBagaglio();
        } catch (IOException e) {
            logger.severe(String.format("Errore nella lista bagagli: %s", e.getMessage()));
        }
        for (Luggage info : listaBagagli) { //per ogni bagaglio della lista
            if (info.getCompagnia().equals(compagnia)) { //se la compagnia del bagaglio corrisponde a quella inserita
                invioMsg(chatId, info.toString()); //invia un messaggio con info bagaglio
                return;
            }
        }
        invioMsg(chatId, "❌ Informazioni non trovate per " + compagnia);
    }

    private InlineKeyboardButton creaBottone(String testo, String callbackData) {
        InlineKeyboardButton bottone = new InlineKeyboardButton(testo); //crea un bottone
        bottone.setCallbackData(callbackData); //con callbackData associato
        return bottone;
    }

    private void getInfoVolo(long chatId, String numeroVolo) {
        invioMsg(chatId, "🔍 Ricerca informazioni per il volo " + numeroVolo + "...");

        Flight volo = flightService.getInfoVolo(numeroVolo);

        if (volo == null) {
            invioMsg(chatId, "❌ Volo non trovato. Possibili cause:\n" +
                    "• Numero volo errato\n" +
                    "• Il volo non è in programma oggi\n" +
                    "• Limite API raggiunto (500 richieste/mese)\n\n" +
                    "Riprova con il formato: CODICE_COMPAGNIA + NUMERO (es. AZ123)");
            return;
        }
        invioMsg(chatId, volo.toString());

        SendMessage msg = new SendMessage(String.valueOf(chatId), "Vuoi altre informazioni?");

        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        //keyboard.add(new InlineKeyboardRow(creaBottone("🗺️ Mostra Mappa", "view_map:" + numeroVolo)));
        keyboard.add(new InlineKeyboardRow(creaBottone("📍 Traccia Volo", "track_flight:" + numeroVolo)));
        if (volo.getIcao24() != null)
            keyboard.add(new InlineKeyboardRow(creaBottone("✈️ Immagine Aereo", "view_aircraft:" + volo.getIcao24())));
        else if (volo.getRegistrazioneAereo() != null)
            keyboard.add(new InlineKeyboardRow(creaBottone("✈️ Immagine Aereo", "view_aircraft:" + volo.getRegistrazioneAereo())));
        else if (volo.getTipoAereo() != null)
            keyboard.add(new InlineKeyboardRow(creaBottone("✈️ Immagine Aereo", "view_aircraft:" + volo.getTipoAereo())));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(keyboard);
        msg.setReplyMarkup(markup); //invia i pulsanti insieme al messaggio

        try {
            telegramClient.execute(msg);
        } catch (TelegramApiException e) {
            logger.severe(String.format("Errore nell'invio pulsanti info aggiuntive: %s", e.getMessage()));
        }
        if (volo.getIataCompagnia() != null) {
            File logo = imageService.downloadLogoCompagnia(volo.getIataCompagnia()); //scarica logo compagnia
            if (logo != null)
                invioFoto(chatId, logo, "Logo " + volo.getCopagnia()); //lo invia con nome compagnia
        }
    }

    private void tracciaVolo(long chatId, String numeroVolo) {
        try {
            DatabaseManager.getInstance().aggiungiVoloTracciato(chatId, numeroVolo); //aggiunge volo al db
            int intervallo = DatabaseManager.getInstance().getIntervalloMinuti(chatId);
            notificationScheduler.pianificaControllo(chatId, numeroVolo, intervallo, telegramClient); //pianifica notifica ricorrente

            invioMsg(chatId, "✅ Volo " + numeroVolo + " aggiunto al tracking!\nRiceverai aggiornamenti ogni " + intervallo + " minuti.");
        } catch (Exception e) {
            logger.severe(String.format("Errore tracciamento volo: %s", e.getMessage()));
            invioMsg(chatId, "❌ Errore nell'aggiungere il volo al tracking.");
        }
    }

    private void getInfoAeroporto(long chatId, String codiceIata) {
        invioMsg(chatId, "🔍 Ricerca informazioni per l'aeroporto " + codiceIata + "...");

        Airport aeroporto = airportService.getInfoAeroportoServ(codiceIata);

        if (aeroporto == null) {
            invioMsg(chatId, "❌ Aeroporto non trovato.\n\n" +
                    "Verifica:\n" +
                    "• Codice IATA corretto (3 lettere, es. MXP)\n" +
                    "• Limite API: 150 richieste/giorno\n" +
                    "• Verifica la chiave API RapidAPI");
            return;
        }

        StringBuilder info = new StringBuilder(aeroporto.toString());

        if (aeroporto.getLatitudine() != null && aeroporto.getLongitudine() != null) {
            Weather meteo = weatherService.getMeteoDaCoord(aeroporto.getLatitudine(), aeroporto.getLongitudine());
            if (meteo != null) {
                info.append(meteo.toString());
            }
        }
        invioMsg(chatId, info.toString());
    }

    private void getInfoMeteo(long chatId, String citta) {
        Weather meteo = weatherService.getMeteo(citta);

        if (meteo == null) {
            invioMsg(chatId, "❌ Meteo non disponibile per " + citta + "\n\n" +
                    "Verifica:\n" +
                    "• Nome città corretto\n" +
                    "• Limite API: 1000 richieste/giorno\n" +
                    "• Prova con il nome in inglese (es. Milan invece di Milano)");
            return;
        }
        invioMsg(chatId, meteo.toString());
    }

    private void cercaBiglietti(long chatId, String da, String a, String data) {
        invioMsg(chatId, "🔍 Ricerca offerte voli...");

        List<Ticket> offerte = amadeusService.cercaOfferteVolo(da, a, data, 1);

        if (offerte.isEmpty()) {
            invioMsg(chatId, "❌ Nessuna offerta trovata.\n\n" +
                    "Note:\n" +
                    "• Usa codici IATA validi (3 lettere)\n" +
                    "• Data formato YYYY-MM-DD\n" +
                    "• Funziona meglio con aeroporti principali");
            return;
        }
        StringBuilder risultato = new StringBuilder("🎫 Offerte trovate:\n\n");
        int count = 0;
        for (Ticket biglietto : offerte) {
            if (count == 3) break; //si ferma dopo averne elaborati 3

            risultato.append(biglietto.toString());
            risultato.append("────────────────\n");

            count++;
        }
        invioMsg(chatId, risultato.toString());
    }

    private void gestisciNotifiche(long chatId, String numeroVolo, int minuti) {
        long millis = System.currentTimeMillis() + (minuti * 60 * 1000L); //converte min in millisecondi, L per long
        Timestamp tempoNotifica = new Timestamp(millis);

        String msg = "🔔 Promemoria per il volo " + numeroVolo;

        notificationScheduler.programmaNotificaVolo(chatId, numeroVolo, tempoNotifica, msg, telegramClient);

        invioMsg(chatId, "✅ Notifica programmata tra " + minuti + " minuti per il volo " + numeroVolo);
    }

    private void mostraVoliTracciati(long chatId) {
        try {
            ResultSet rs = DatabaseManager.getInstance().getVoliTracciati(chatId);

            StringBuilder risultato = new StringBuilder("📋 I tuoi voli tracciati:\n\n");
            boolean haVoli = false;

            while (rs.next()) {
                haVoli = true;
                String numeroVolo = rs.getString("flight_number");
                risultato.append("✈️ ").append(numeroVolo).append("\n");
            }
            if (!haVoli)
                risultato.append("Nessun volo tracciato. Usa /track <numero> per aggiungerne uno.");

            invioMsg(chatId, risultato.toString());
        } catch (Exception e) {
            logger.severe(String.format("Error showing tracked flights: %s", e.getMessage()));
            invioMsg(chatId, "❌ Errore nel recuperare i voli tracciati.");
        }
    }

    private void sendFlightMap(long chatId, String flightNumber) {
        Flight flight = flightService.getInfoVolo(flightNumber);

        if (flight == null) {
            invioMsg(chatId, "❌ Impossibile generare la mappa.");
            return;
        }

        if (flight.getLatitudine() != null && flight.getLongitudine() != null) {
            File map = mapService.generateLiveTrackingMap(
                    flight.getLatitudine(),
                    flight.getLongitudine(),
                    flightNumber,
                    flight.getAltitudine() != null ? flight.getAltitudine() : 0,
                    flight.getVelocita() != null ? flight.getVelocita() : 0
            );

            if (map != null) {
                invioFoto(chatId, map, "Posizione volo " + flightNumber);
            }
        } else {
            invioMsg(chatId, "❌ Posizione non disponibile per questo volo.");
        }
    }

    private void inviaFotoAereo(long chatId, String IdAereo) {
        File immagine = imageService.scaricaImmagineAereo(IdAereo);
        if (immagine != null) {
            invioFoto(chatId, immagine, "Aeromobile " + IdAereo);
        } else {
            invioMsg(chatId, "❌ Immagine non disponibile.");
        }
    }

    private void invioMsg(long chatId, String testo) {
        SendMessage msg = new SendMessage(String.valueOf(chatId), testo);

        try {
            telegramClient.execute(msg);
        } catch (TelegramApiException e) {
            logger.severe(String.format("Errore nell'invio messaggio: %s", e.getMessage()));
        }
    }

    private void inviaMessaggioBottoni(long chatId, String testo) {
        SendMessage msg = new SendMessage(String.valueOf(chatId), testo);

        KeyboardRow rig1 = new KeyboardRow("/flight", "/track", "/settings");
        KeyboardRow rig2 = new KeyboardRow("/airport", "/weather", "/luggage");
        KeyboardRow rig3 = new KeyboardRow("/tickets", "/myflights", "/notify");

        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .keyboardRow(rig1)
                .keyboardRow(rig2)
                .keyboardRow(rig3)
                .resizeKeyboard(true)
                .build();
        msg.setReplyMarkup(keyboardMarkup);

        try {
            telegramClient.execute(msg);
        } catch (TelegramApiException e) {
            logger.severe(String.format("Errore nell'invio messaggio con tastiera: %s", e.getMessage()));
        }
    }

    private void invioFoto(long chatId, File foto, String descrizione) {
        SendPhoto sendPhoto = new SendPhoto(String.valueOf(chatId), new InputFile(foto));
        sendPhoto.setCaption(descrizione);

        try {
            telegramClient.execute(sendPhoto);
        } catch (TelegramApiException e) {
            logger.severe(String.format("Errore nell'invio foto: %s", e.getMessage()));
        }
    }

    public void chiusura() {
        notificationScheduler.shutdown();
        DatabaseManager.getInstance().close();
    }
}
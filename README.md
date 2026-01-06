# ✈️ FlyAdvisor (@FlyAdvisorBot)

FlyAdvisorBot è un bot Telegram per la gestione e il monitoraggio dei voli. Fornisce informazioni in tempo reale sui voli, aeroporti, condizioni meteorologiche, offerte di biglietti e molto altro, con un'interfaccia dotata di pulsanti.

## ✨ Caratteristiche Principali

- **✈️ Tracciamento Voli**: Monitora lo stato dei voli in tempo reale
- **🏢 Informazioni Aeroporti**: Dettagli completi sugli aeroporti (codici IATA, posizioni, ecc.)
- **🌤️ Previsioni Meteo**: Condizioni meteorologiche per città e aeroporti
- **🎫 Ricerca Biglietti**: Offerte di volo 
- **🔔 Notifiche Personalizzate**: Avvisi programmati per i voli tracciati
- **🧳 Gestione Bagagli**: Informazioni sulle dimensioni e i pesi dei bagagli per compagnia
- **🖼️ Immagini compagnie e aerei**: Loghi delle compagnie aeree e foto degli aerei cercati
- **🗺️ Mappe Interattive**: Visualizzazione tratte di volo e tracking live su mappe reali
- **💾 Database Locale**: Salvataggio dei voli tracciati e preferenze utente
- **📝 Logging**: logging di errori e azioni utente varie

## 🔗 API Utilizzate

Il bot integra molteplici API esterne per fornire i dati:

- **🤖 Telegram Bot API**: Per l'interazione con gli utenti Telegram
- **✈️ AviationStack API**: Per informazioni dettagliate sui voli (stato, orari, posizioni)
- **🏢 AeroDataBox API**: Per dati sugli aeroporti (posizioni, codici, informazioni generali)
- **🌤️ OpenWeatherMap API**: Per previsioni meteorologiche
- **🎫 Amadeus API**: Per ricerca e prenotazione offerte di volo
- **🗺️ OpenStreetMap**: Per generazione mappe interattive con piastrelle geografiche
- **🖼️ Kiwi.com Images**: Per loghi delle compagnie aeree
- **🌐 Web Scraping (Jsoup)**: Per informazioni sulle restrizioni bagagli da siti web
- **📸 Plane Spotters**: Per foto degli aerei

## Architettura del Progetto

### Struttura delle Directory
```
src/main/java/com/flightbot/
├── bot/
│   └── FlyAdvisorBot.java          # Classe principale del bot
├── config/
│   └── ConfigLoader.java           # Gestione configurazione
├── database/
│   └── DatabaseManager.java        # Interazione con database SQLite
├── models/
│   ├── Airport.java                # Modello dati aeroporto
│   ├── Flight.java                 # Modello dati volo
│   ├── Luggage.java                # Modello dati bagagli
│   ├── Ticket.java                 # Modello dati biglietto
│   └── Weather.java                # Modello dati meteo
└── services/
    ├── AirportService.java         # Servizio aeroporti (AeroDataBox)
    ├── AmadeusService.java         # Servizio offerte voli (Amadeus)
    ├── FlightService.java          # Servizio info voli (AviationStack)
    ├── ImageService.java           # Servizio immagini e loghi
    ├── LuggageService.java         # Servizio info bagagli (web scraping)
    ├── MapService.java             # Servizio generazione mappe (OpenStreetMap)
    ├── NotificationScheduler.java  # Servizio notifiche (Quartz)
    └── WeatherService.java         # Servizio meteo (OpenWeatherMap)
```

### Dipendenze Principali
- **TelegramBots**: Libreria Java per Telegram Bot API
- **OkHttp**: Client HTTP per chiamate API
- **Gson**: Parsing JSON
- **SQLite**: Database locale
- **Quartz Scheduler**: Pianificazione notifiche
- **Jsoup**: Web scraping
- **Apache Commons Configuration**: Gestione file configurazione

## Flusso dell'Applicazione

### Avvio
1. Caricamento configurazione da `conf.properties`
2. Inizializzazione database SQLite
3. Setup scheduler per notifiche
4. Registrazione bot con Telegram API
5. Avvio polling per messaggi

### Gestione Messaggi
Il bot utilizza un sistema di stati per gestire gli input multi-step:

#### Sistema di Stati (AWAITING)
- `AWAITING_FLIGHT_NUMBER`: In attesa numero volo per tracciamento
- `AWAITING_FLIGHT_INFO`: In attesa numero volo per informazioni
- `AWAITING_AIRPORT_CODE`: In attesa codice IATA aeroporto
- `AWAITING_WEATHER_CITY`: In attesa nome città per meteo
- `AWAITING_TICKET_FROM`: In attesa aeroporto partenza per ricerca biglietti
- `AWAITING_TICKET_TO`: In attesa aeroporto destinazione
- `AWAITING_TICKET_DATE`: In attesa data viaggio
- `AWAITING_NOTIFY_FLIGHT`: In attesa numero volo per notifica
- `AWAITING_NOTIFY_TIME`: In attesa minuti per notifica
- `AWAITING_NOTIFICATION_INTERVAL`: In attesa intervallo notifiche

#### Callback Query System
Le interazioni tramite pulsanti inline utilizzano il formato `azione:parametro`:
- `menu:FLIGHT` - Menu volo
- `view_map:NUMERO` - Visualizza mappa del volo (tratta o posizione live)
- `view_aircraft:ID` - Visualizza immagine aereo
- `track_flight:NUMERO` - Traccia volo specifico
- `luggage:COMPAGNIA` - Info bagagli compagnia
- `settings:AZIONE` - Gestione impostazioni

### Elaborazione Comandi
1. **Riconoscimento**: Controllo se messaggio inizia con `/`
2. **Parsing**: Separazione comando e argomenti
3. **Routing**: Smistamento al metodo appropriato
4. **Stato**: Impostazione stato conversazione se necessario
5. **Risposta**: Invio risposta formattata all'utente

## 📋 Comandi Disponibili

### 🤖 Comandi Base
- `/start` - Messaggio di benvenuto e introduzione
- `/help` - Lista completa dei comandi disponibili
- `/menu` - Menu interattivo con pulsanti

### ✈️ Funzionalità Voli
- `/track [numero_volo]` - Traccia un volo specifico
- `/flight [numero_volo]` - Ottieni informazioni dettagliate su un volo
- `/myflights` - Mostra lista dei voli tracciati

### ℹ️ Informazioni
- `/airport [codice_IATA]` - Informazioni su un aeroporto
- `/weather [città]` - Previsioni meteorologiche

### 🎫 Ricerca e Prenotazione
- `/tickets` - Ricerca offerte di volo (guidata)
- `/notify [numero_volo]` - Imposta notifica per un volo

### 🛠️ Utilità
- `/luggage` - Informazioni sulle restrizioni bagagli
- `/settings` - Gestione preferenze utente

## 🔧 Funzionalità Dettagliate

### ✈️ Tracciamento Voli
- Monitoraggio stato volo in tempo reale
- Salvataggio automatico nel database
- Notifiche programmate per aggiornamenti
- Immagini degli aeromobili

### 🔔 Sistema Notifiche
- Notifiche personalizzate per voli tracciati
- Intervallo configurabile (1-1440 minuti)
- Attivazione/disattivazione notifiche per utente
- Utilizzo Quartz Scheduler per pianificazione

### 🎫 Ricerca Biglietti
- Ricerca per data, origine, destinazione
- Numero adulti configurabile
- Visualizzazione offerte

### 🗺️ Servizio Mappe Interattive
- Visualizzazione della rotta completa tra aeroporti di partenza e arrivo
- Monitoraggio posizione corrente dell'aereo su mappa geografica
- Dettagli volo (numero, altitudine, velocità) direttamente sulla mappa

### 💾 Gestione Database
- Database SQLite locale (`data/flyadvisor.db`)
- Tabelle per voli tracciati, preferenze utente

#### 📊 Schema Database

**tracked_flights** - Tabella per memorizzare i voli tracciati dagli utenti
- Colonne: id, chat_id, flight_number, created_at, active

**scheduled_notifications** - Tabella per le notifiche programmate relative ai voli
- Colonne: id, chat_id, flight_number, notification_time, message, sent, created_at

**user_preferences** - Tabella per le preferenze personali degli utenti
- Colonne: chat_id, language, notifications_enabled, notification_interval_minutes, created_at, updated_at

### 🎨 Interfaccia Utente
- Tastiere inline per navigazione intuitiva
- Formattazione messaggi con emoji
- Gestione errori e messaggi di fallback

## ⚙️ Configurazione

### 📄 File `conf.properties`
Contiene:
- Token bot Telegram
- Chiavi API per tutti i servizi
- Configurazione database
- Impostazioni scheduler

⚠️IMPORTANTE: Per il corretto funzionamento del bot, rinominare il file confPUBBLICO.properties in conf.properties e compilarlo con le proprie credenziali API Key.

## 👤 Autore
[![mameledss](https://github.com/mameledss.png?size=30)](https://github.com/mameledss)
**[mameledss](https://github.com/mameledss)**
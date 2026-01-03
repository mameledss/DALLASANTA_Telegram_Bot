package com.flightbot.database;

import com.flightbot.config.ConfigLoader;

import java.util.logging.Logger;

import java.sql.*;

public class DatabaseManager {
    private static final Logger logger = Logger.getLogger(DatabaseManager.class.getName());
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection(
                    ConfigLoader.getDatabaseUrl()
            );
            inizializzaTabelle();
            logger.info("Database inizializzato correttamente");
        } catch (Exception e) {
            logger.severe(String.format("Errore nell'inizializzazione del database: %s", e.getMessage()));
            throw new RuntimeException(e);
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private boolean isConnessioneValida() {
        try {
            return connection != null && connection.isValid(5);
        } catch (SQLException e) {
            logger.severe(String.format("Errore controllo validità connessione: %s", e.getMessage()));
            return false;
        }
    }

    private void inizializzaTabelle() throws SQLException {
        //tabella per tracking voli
        eseguiUpdate("""
            CREATE TABLE IF NOT EXISTS tracked_flights (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                chat_id INTEGER NOT NULL,
                flight_number TEXT NOT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                active BOOLEAN DEFAULT 1
            )
        """);

        //tabella per notifiche programmate
        eseguiUpdate("""
            CREATE TABLE IF NOT EXISTS scheduled_notifications (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                chat_id INTEGER NOT NULL,
                flight_number TEXT NOT NULL,
                notification_time DATETIME NOT NULL,
                message TEXT,
                sent BOOLEAN DEFAULT 0,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """);

        //tabella per preferenze utente
        eseguiUpdate("""
            CREATE TABLE IF NOT EXISTS user_preferences (
                chat_id INTEGER PRIMARY KEY,
                language TEXT DEFAULT 'it',
                notifications_enabled BOOLEAN DEFAULT 1,
                notification_interval_minutes INTEGER DEFAULT 15,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """);

        try { //aggiunge colonna notification_interval_minutes se non esiste
            eseguiUpdate("ALTER TABLE user_preferences ADD COLUMN notification_interval_minutes INTEGER DEFAULT 15");
        } catch (SQLException e) {
            //la colonna potrebbe già esistere, ignora l'errore
        }
    }

    public void eseguiUpdate(String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return connection.prepareStatement(sql);
    }

    public Connection getConnection() {
        return connection;
    }

    public void aggiungiVoloTracciato(long chatId, String numeroVolo) throws SQLException {
        if (!isConnessioneValida()) {
            logger.severe("Connessione al database non valida");
            return;
        }
        String sql = "INSERT INTO tracked_flights (chat_id, flight_number) VALUES (?, ?)";
        try (PreparedStatement stmt = prepareStatement(sql)) {
            stmt.setLong(1, chatId);
            stmt.setString(2, numeroVolo);
            stmt.executeUpdate();
        }
    }

    public void removeTrackedFlight(long chatId, String numeroVolo) throws SQLException {
        if (!isConnessioneValida()) {
            logger.severe("Connessione al database non valida");
            return;
        }
        String sql = "UPDATE tracked_flights SET active = FALSE WHERE chat_id = ? AND flight_number = ?";
        try (PreparedStatement stmt = prepareStatement(sql)) {
            stmt.setLong(1, chatId);
            stmt.setString(2, numeroVolo);
            stmt.executeUpdate();
        }
    }

    public ResultSet getVoliTracciati(long chatId) throws SQLException {
        if (!isConnessioneValida()) {
            logger.severe("Connessione al database non valida");
            return null;
        }
        String sql = "SELECT * FROM tracked_flights WHERE chat_id = ? AND active = TRUE";
        try (PreparedStatement stmt = prepareStatement(sql)) {
            stmt.setLong(1, chatId);
            return stmt.executeQuery();
        }
    }

    public void scheduleNotification(long chatId, String numeroVolo, Timestamp tempoNotifica, String msg) throws SQLException {
        if (!isConnessioneValida()) {
            logger.severe("Connessione al database non valida");
            return;
        }
        String sql = "INSERT INTO scheduled_notifications (chat_id, flight_number, notification_time, message) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = prepareStatement(sql)) {
            stmt.setLong(1, chatId);
            stmt.setString(2, numeroVolo);
            stmt.setTimestamp(3, tempoNotifica);
            stmt.setString(4, msg);
            stmt.executeUpdate();
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Connessione al database chiusa");
            }
        } catch (SQLException e) {
            logger.severe(String.format("Errore chiusura connessione al database: %s", e.getMessage()));
        }
    }

    //metodi per gestire le preferenze utente
    public void setPreferenzeUtente(long chatId, boolean notificheAbilitate, int intervalloMinutiNotifiche) throws SQLException {
        if (!isConnessioneValida()) {;
            logger.severe("Connessione al database non valida");
            return;
        }
        String sql = """
            INSERT INTO user_preferences (chat_id, notifications_enabled, notification_interval_minutes, updated_at)
            VALUES (?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT(chat_id) DO UPDATE SET
                notifications_enabled = excluded.notifications_enabled,
                notification_interval_minutes = excluded.notification_interval_minutes,
                updated_at = CURRENT_TIMESTAMP
        """;
        try (PreparedStatement stmt = prepareStatement(sql)) {
            stmt.setLong(1, chatId);
            stmt.setBoolean(2, notificheAbilitate);
            stmt.setInt(3, intervalloMinutiNotifiche);
            stmt.executeUpdate();
        }
    }

    public boolean areNotificheAbilitate(long chatId) throws SQLException {
        if (!isConnessioneValida()) {
            logger.severe("Connessione al database non valida");
            return true; //default a true se non riusce a leggere
        }
        String sql = "SELECT notifications_enabled FROM user_preferences WHERE chat_id = ?";
        try (PreparedStatement stmt = prepareStatement(sql)) {
            stmt.setLong(1, chatId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("notifications_enabled");
            }
        }
        return true; //default a true se non ci sono preferenze salvate
    }

    public int getIntervalloMinuti(long chatId) throws SQLException {
        if (!isConnessioneValida()) {
            logger.severe("Connessione al database non valida");
            return 15; //default a 15 minuti se non riusce a leggere
        }
        String sql = "SELECT notification_interval_minutes FROM user_preferences WHERE chat_id = ?";
        try (PreparedStatement stmt = prepareStatement(sql)) {
            stmt.setLong(1, chatId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("notification_interval_minutes");
            }
        }
        return 15; //default a 15 minuti se non ci sono preferenze salvate
    }

    public ResultSet getUserPreferences(long chatId) throws SQLException {
        if (!isConnessioneValida()) {
            logger.severe("Connessione al database non valida");
            return null;
        }
        String sql = "SELECT * FROM user_preferences WHERE chat_id = ?";
        try (PreparedStatement stmt = prepareStatement(sql)) {
            stmt.setLong(1, chatId);
            return stmt.executeQuery();
        }
    }
}

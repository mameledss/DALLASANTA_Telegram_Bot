package com.flightbot;

import com.flightbot.bot.FlyAdvisorBot;
import com.flightbot.config.ConfigLoader;
import org.quartz.SchedulerException;
import java.util.logging.Logger;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        logger.info("Avvio FlyAdvisor Bot...");

        String botToken = ConfigLoader.getTelegramBotToken(); //ottiene il token dal file di conf
        //long polling = bot chiede continuamente a Telegram se ci sono nuovi messaggi
        TelegramBotsLongPollingApplication botsApplication = null; //inizializza il gestore dei bot
        FlyAdvisorBot bot = null; //inizializza il bot

        try {
            botsApplication = new TelegramBotsLongPollingApplication();
            bot = new FlyAdvisorBot();
            botsApplication.registerBot(botToken, bot); //registra il bot presso telegram

            logger.info("FlyAdvisor Bot avviato correttamente!");
            logger.info(String.format("Bot username: %s", ConfigLoader.getTelegramBotUsername()));
            //variabili "final" per consentire la funzione lambda di chiusura
            FlyAdvisorBot botFinale = bot;
            TelegramBotsLongPollingApplication botsAppFinale = botsApplication;

            Runtime.getRuntime().addShutdownHook(new Thread(() -> { //registra un hook eseguito quando il programma sta per terminare
                logger.info("Terminando FlyAdvisor Bot...");
                botFinale.chiusura();
                try {
                    botsAppFinale.close();
                } catch (Exception e) {
                    logger.severe(String.format("Errore chiusura bot: %s", e.getMessage()));
                }
                logger.info("FlyAdvisor Bot terminato.");
            }));

            logger.info("Bot in esecuzione. Premi Ctrl+C per terminare.");
            Thread.currentThread().join(); //mantiene thread in esecuzione continuamente

        } catch (TelegramApiException e) {
            logger.severe(String.format("Errore nell'avviare Telegram Bot API: %s", e.getMessage()));
            System.exit(-1);
        } catch (SchedulerException e) {
            logger.severe(String.format("Errore nell'avviare lo scheduler: %s", e.getMessage()));
            System.exit(-1);
        } catch (InterruptedException e) {
            logger.info("Bot interrotto dall'utente");
            if (bot != null) bot.chiusura();
            if (botsApplication != null) {
                try {
                    botsApplication.close();
                } catch (Exception ex) {
                    logger.severe(String.format("Errore chiusura bot: %s", ex.getMessage()));
                }
            }
        } catch (Exception e) {
            logger.severe(String.format("Errore inaspettato durante l'avvio: %s", e.getMessage()));
            System.exit(-1);
        }
    }
}
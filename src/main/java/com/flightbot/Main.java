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

        String botToken = ConfigLoader.getTelegramBotToken();
        TelegramBotsLongPollingApplication botsApplication = null;
        FlyAdvisorBot bot = null;

        try {
            botsApplication = new TelegramBotsLongPollingApplication();
            bot = new FlyAdvisorBot();
            botsApplication.registerBot(botToken, bot);

            logger.info("FlyAdvisor Bot avviato correttamente!");
            logger.info(String.format("Bot username: %s", ConfigLoader.getTelegramBotUsername()));

            FlyAdvisorBot botFinale = bot;
            TelegramBotsLongPollingApplication botsAppFinale = botsApplication;

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
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
            Thread.currentThread().join(); //thread in esecuzione

        } catch (TelegramApiException e) {
            logger.severe(String.format("Errore nell'avviare Telegram Bot API: %s", e.getMessage()));
            e.printStackTrace();
            System.exit(-1);
        } catch (SchedulerException e) {
            logger.severe(String.format("Errore nell'avviare lo scheduler: %s", e.getMessage()));
            e.printStackTrace();
            System.exit(-1);
        } catch (InterruptedException e) {
            logger.info("Bot interrotto dall'utente.");
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
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
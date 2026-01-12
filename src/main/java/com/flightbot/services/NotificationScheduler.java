package com.flightbot.services;

import com.flightbot.database.DatabaseManager;
import com.flightbot.models.Flight;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import java.util.logging.Logger;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class NotificationScheduler {
    private static final Logger logger = Logger.getLogger(NotificationScheduler.class.getName());
    private final Scheduler scheduler;

    public NotificationScheduler() throws SchedulerException {
        this.scheduler = StdSchedulerFactory.getDefaultScheduler(); //usa quartz
        this.scheduler.start();
        logger.info("Scheduler notifiche avviato");
    }

    public void programmaNotificaVolo(long chatId, String numeroVolo, Timestamp tempoNotifica, String msg, Object bot) {
        try {
            JobDataMap jobDataMap = new JobDataMap(); //crea "mappa" di lavoro con dati da passare
            jobDataMap.put("chatId", chatId);
            jobDataMap.put("message", msg);
            jobDataMap.put("bot", bot);

            JobDetail job = JobBuilder.newJob(NotificationJob.class) //crea il lavoro da eseguire in futuro rispetto alla classe NotificationJob
                    .withIdentity("notification_" + chatId + "_" + System.currentTimeMillis()) //identificatore univoco
                    .usingJobData(jobDataMap) //i dati preparati prima vengono associati a questo job
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger() //crea trigger che determina quando eseguire il job
                    .withIdentity("trigger_" + chatId + "_" + System.currentTimeMillis())
                    .startAt(tempoNotifica) //si attiva al tempo specificato
                    .build();

            scheduler.scheduleJob(job, trigger); //registra job e trigger nello scheduler di quartz

            DatabaseManager.getInstance().programmaNotifica(chatId, numeroVolo, tempoNotifica, msg); //salva i dettagli della notifica nel db

            logger.info(String.format("Notifica impostata per chat %d alle %s", chatId, tempoNotifica));
        } catch (Exception e) {
            logger.severe(String.format("Fallita impostazione notifica: %s", e.getMessage()));
        }
    }

    public void pianificaControllo(long chatId, String numeroVolo, int intervalloMinuti, Object bot) {
        try {
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("chatId", chatId);
            jobDataMap.put("flightNumber", numeroVolo);
            jobDataMap.put("bot", bot);

            JobDetail job = JobBuilder.newJob(FlightCheckJob.class)
                    .withIdentity("flight_check_" + chatId + "_" + numeroVolo)
                    .usingJobData(jobDataMap)
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger_check_" + chatId + "_" + numeroVolo)
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInMinutes(intervalloMinuti)
                            .repeatForever()) //viene ripetuto all'infinito
                    .build();

            scheduler.scheduleJob(job, trigger);

            logger.info(String.format("Check periodico programmato per volo %s ogni %d minuti", numeroVolo, intervalloMinuti));
        } catch (SchedulerException e) {
            logger.severe(String.format("Fallita impostazione check periodico: %s", e.getMessage()));
        }
    }

    public void cancelNotification(String jobId) {
        try {
            scheduler.deleteJob(new JobKey(jobId));
            logger.info(String.format("Notifica cancellata %s", jobId));
        } catch (SchedulerException e) {
            logger.severe(String.format("Errore nel cancellare notifica: %s", e.getMessage()));
        }
    }

    public void cancellaVoloCheck(long chatId, String numeroVolo) {
        try {
            scheduler.deleteJob(new JobKey("flight_check_" + chatId + "_" + numeroVolo));
            logger.info(String.format("Check periodico cancellato per volo %s da chat %d", numeroVolo, chatId));
        } catch (SchedulerException e) {
            logger.severe(String.format("Errore nel cancellare check periodico: %s", e.getMessage()));
        }
    }

    public void ripristinaVoliTracciati(DatabaseManager db, int intervalloMinutiDefault, Object bot) {
        try {
            ResultSet rs = db.getTuttiVoliTracciati(); //legge tutti i voli tracciati attivi dal database
            
            if (rs != null) {
                while (rs.next()) {
                    long chatId = rs.getLong("chat_id");
                    String numeroVolo = rs.getString("flight_number");
                    
                    try {
                        //recupera l'intervallo personalizzato dell'utente, o usa il default
                        int intervalloUtente = intervalloMinutiDefault;
                        try {
                            intervalloUtente = db.getIntervalloMinuti(chatId);
                        } catch (Exception e) {
                            logger.warning(String.format("Impossibile recuperare intervallo per chat %d, uso default", chatId));
                        }
                        pianificaControllo(chatId, numeroVolo, intervalloUtente, bot);
                    } catch (Exception e) {
                        logger.warning(String.format("Impossibile riprogrammare volo %s per chat %d: %s", numeroVolo, chatId, e.getMessage()));
                    }
                }
                rs.close();
            }
            logger.info("Ripristino voli tracciati completato");
        } catch (SQLException e) {
            logger.severe(String.format("Errore nel ripristino voli tracciati: %s", e.getMessage()));
        }
    }

    public void spegni() {
        try {
            scheduler.shutdown(); //ferma lo scheduler
            logger.info("Scheduler notifiche fermato");
        } catch (SchedulerException e) {
            logger.severe(String.format("Errore nel fermare scheduler notifiche: %s", e.getMessage()));
        }
    }

    public static class NotificationJob implements Job { //si occupa di inviare la notifica
        private static final Logger logger = Logger.getLogger(NotificationJob.class.getName());

        @Override
        public void execute(JobExecutionContext context) {
            JobDataMap dataMap = context.getJobDetail().getJobDataMap(); //recuper i dati dalla dataMap
            long chatId = dataMap.getLong("chatId");
            String msg = dataMap.getString("message");
            TelegramClient telegramClient = (TelegramClient) dataMap.get("bot");

            try {
                SendMessage sendMessage = new SendMessage(String.valueOf(chatId), msg);

                telegramClient.execute(sendMessage);
                logger.info(String.format("Notifica inviata a chat %d", chatId));
            } catch (Exception e) {
                logger.severe(String.format("Errore invio notifica: %s", e.getMessage()));
            }
        }
    }

    public static class FlightCheckJob implements Job { //si occupa di controllare lo stato del volo
        private static final Logger logger = Logger.getLogger(FlightCheckJob.class.getName());

        @Override
        public void execute(JobExecutionContext context) {
            JobDataMap dataMap = context.getJobDetail().getJobDataMap(); //recuper i dati dalla dataMap
            long chatId = dataMap.getLong("chatId");
            String numeroVolo = dataMap.getString("flightNumber");
            TelegramClient telegramClient = (TelegramClient) dataMap.get("bot");

            try {
                FlightService flightService = new FlightService(); //crea un oggetto FlightService
                Flight volo = flightService.getInfoVolo(numeroVolo); //ottiene le info del volo

                if (volo != null) {
                    String messaggioStato = String.format(
                            "✈️ Aggiornamento %s\n\n" +
                                    "Status: %s\n" +
                                    "Partenza: %s\n" +
                                    "Arrivo: %s\n" +
                                    "%s",
                            numeroVolo,
                            volo.getStato(),
                            volo.getOrarioPartenza() != null ? formattaDataOra(volo.getOrarioPartenza()) : "N/A",
                            volo.getOrarioArrivo() != null ? formattaDataOra(volo.getOrarioArrivo()) : "N/A",
                            volo.getRitardo() != null && volo.getRitardo() > 0 ?
                                    "⚠️ Ritardo: " + volo.getRitardo() + " minuti" : ""
                    );
                    SendMessage sendMessage = new SendMessage(String.valueOf(chatId), messaggioStato);

                    telegramClient.execute(sendMessage); //invia il messaggio
                }
            } catch (Exception e) {
                logger.severe(String.format("Failed to check flight status: %s", e.getMessage()));
            }
        }
        private String formattaDataOra(String isoDataOra) { //formatta data/ora ISO 8601 in formato leggibile. 2024-01-15T10:30:00 -> 15/01/2024 10:30
            try {
                String[] parti = isoDataOra.split("T");
                String parteData = parti[0];
                String parteOra = parti[1].substring(0, 5); //HH:MM

                String[] partiData = parteData.split("-");
                return partiData[2] + "/" + partiData[1] + "/" + partiData[0] + " " + parteOra;
            } catch (Exception e) {
                return isoDataOra;
            }
        }
    }
}

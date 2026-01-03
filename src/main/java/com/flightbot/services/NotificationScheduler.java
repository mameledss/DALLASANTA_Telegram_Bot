package com.flightbot.services;

import com.flightbot.database.DatabaseManager;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import java.util.logging.Logger;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.sql.Timestamp;

public class NotificationScheduler {
    private static final Logger logger = Logger.getLogger(NotificationScheduler.class.getName());
    private final Scheduler scheduler;

    public NotificationScheduler() throws SchedulerException {
        this.scheduler = StdSchedulerFactory.getDefaultScheduler();
        this.scheduler.start();
        logger.info("Notification scheduler started");
    }

    public void programmaNotificaVolo(long chatId, String flightNumber, Timestamp notificationTime,
                                      String message, Object bot) {
        try {
            // Create job detail
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("chatId", chatId);
            jobDataMap.put("message", message);
            jobDataMap.put("bot", bot);

            JobDetail job = JobBuilder.newJob(NotificationJob.class)
                    .withIdentity("notification_" + chatId + "_" + System.currentTimeMillis())
                    .usingJobData(jobDataMap)
                    .build();

            // Create trigger
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger_" + chatId + "_" + System.currentTimeMillis())
                    .startAt(notificationTime)
                    .build();

            // Schedule the job
            scheduler.scheduleJob(job, trigger);

            // Save to database
            DatabaseManager.getInstance().scheduleNotification(chatId, flightNumber, notificationTime, message);

            logger.info(String.format("Scheduled notification for chat %d at %s", chatId, notificationTime));
        } catch (Exception e) {
            logger.severe(String.format("Failed to schedule notification: %s", e.getMessage()));
        }
    }

    public void pianificaControllo(long chatId, String flightNumber, int intervalMinutes,
                                   Object bot) {
        try {
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("chatId", chatId);
            jobDataMap.put("flightNumber", flightNumber);
            jobDataMap.put("bot", bot);

            JobDetail job = JobBuilder.newJob(FlightCheckJob.class)
                    .withIdentity("flight_check_" + chatId + "_" + flightNumber)
                    .usingJobData(jobDataMap)
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger_check_" + chatId + "_" + flightNumber)
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInMinutes(intervalMinutes)
                            .repeatForever())
                    .build();

            scheduler.scheduleJob(job, trigger);

            logger.info(String.format("Scheduled recurring check for flight %s every %d minutes", flightNumber, intervalMinutes));
        } catch (SchedulerException e) {
            logger.severe(String.format("Failed to schedule recurring check: %s", e.getMessage()));
        }
    }

    public void cancelNotification(String jobId) {
        try {
            scheduler.deleteJob(new JobKey(jobId));
            logger.info(String.format("Cancelled notification %s", jobId));
        } catch (SchedulerException e) {
            logger.severe(String.format("Failed to cancel notification: %s", e.getMessage()));
        }
    }

    public void shutdown() {
        try {
            scheduler.shutdown();
            logger.info("Notification scheduler shut down");
        } catch (SchedulerException e) {
            logger.severe(String.format("Error shutting down scheduler: %s", e.getMessage()));
        }
    }

    // Job for sending notifications
    public static class NotificationJob implements Job {
        private static final Logger logger = Logger.getLogger(NotificationJob.class.getName());

        @Override
        public void execute(JobExecutionContext context) {
            JobDataMap dataMap = context.getJobDetail().getJobDataMap();
            long chatId = dataMap.getLong("chatId");
            String message = dataMap.getString("message");
            TelegramClient telegramClient = (TelegramClient) dataMap.get("bot");

            try {
                org.telegram.telegrambots.meta.api.methods.send.SendMessage sendMessage =
                        new org.telegram.telegrambots.meta.api.methods.send.SendMessage(
                                String.valueOf(chatId), message);
                telegramClient.execute(sendMessage);
                logger.info(String.format("Notification sent to chat %d", chatId));
            } catch (Exception e) {
                logger.severe(String.format("Failed to send notification: %s", e.getMessage()));
            }
        }
    }

    // Job for checking flight status
    public static class FlightCheckJob implements Job {
        private static final Logger logger = Logger.getLogger(FlightCheckJob.class.getName());

        @Override
        public void execute(JobExecutionContext context) {
            JobDataMap dataMap = context.getJobDetail().getJobDataMap();
            long chatId = dataMap.getLong("chatId");
            String flightNumber = dataMap.getString("flightNumber");
            TelegramClient telegramClient = (TelegramClient) dataMap.get("bot");

            try {
                FlightService flightService = new FlightService();
                com.flightbot.models.Flight flight = flightService.getInfoVolo(flightNumber);

                if (flight != null) {
                    String statusMessage = String.format(
                            "✈️ Aggiornamento %s\n\n" +
                                    "Status: %s\n" +
                                    "Partenza: %s\n" +
                                    "Arrivo: %s\n" +
                                    "%s",
                            flightNumber,
                            flight.getStato(),
                            flight.getOrarioPartenza() != null ? formattaDataOra(flight.getOrarioPartenza()) : "N/A",
                            flight.getOrarioArrivo() != null ? formattaDataOra(flight.getOrarioArrivo()) : "N/A",
                            flight.getRitardo() != null && flight.getRitardo() > 0 ?
                                    "⚠️ Ritardo: " + flight.getRitardo() + " minuti" : ""
                    );

                    org.telegram.telegrambots.meta.api.methods.send.SendMessage sendMessage =
                            new org.telegram.telegrambots.meta.api.methods.send.SendMessage(
                                    String.valueOf(chatId), statusMessage);
                    telegramClient.execute(sendMessage);
                }
            } catch (Exception e) {
                logger.severe(String.format("Failed to check flight status: %s", e.getMessage()));
            }
        }
        private String formattaDataOra(String isoDataOra) { //formatta data/ora ISO 8601 in formato leggibile. 2024-01-15T10:30:00 -> 15/01/2024 10:30
            try {
                String[] parti = isoDataOra.split("T");
                String parteData = parti[0];
                String parteOra = parti[1].substring(0, 5); // HH:MM

                String[] partiData = parteData.split("-");
                return partiData[2] + "/" + partiData[1] + "/" + partiData[0] + " " + parteOra;
            } catch (Exception e) {
                return isoDataOra;
            }
        }
    }
}

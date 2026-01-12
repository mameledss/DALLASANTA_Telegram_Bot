package com.flightbot.models;

import java.util.List;

public class Weather {
    private String city;
    private String description;
    private Double temperature;
    private Double feelsLike;
    private Integer humidity;
    private Double windSpeed;
    private Integer pressure;
    private Integer visibility;
    private String icon;
    private String date; //data per le previsioni future
    private List<Weather> forecast; //previsioni per i prossimi giorni

    public Weather() {}

    public String getCitta() {
        return city;
    }
    public void setCitta(String citta) {
        this.city = citta;
    }

    public String getDescrizione() {
        return description;
    }
    public void setDescrizione(String descrizione) {
        this.description = descrizione;
    }

    public Double getTemperatura() {
        return temperature;
    }
    public void setTemperatura(Double temperatura) {
        this.temperature = temperatura;
    }

    public Double getPercepita() {
        return feelsLike;
    }
    public void setPercepita(Double percepita) {
        this.feelsLike = percepita;
    }

    public Integer getUmidita() { return humidity; }
    public void setUmidita(Integer umidita) { this.humidity = umidita; }

    public Double getVelocitaVento() {
        return windSpeed;
    }
    public void setVelocitaVento(Double velocitaVento) {
        this.windSpeed = velocitaVento;
    }

    public Integer getPressione() {
        return pressure;
    }
    public void setPressione(Integer pressione) {
        this.pressure = pressione;
    }

    public Integer getVisibilita() {
        return visibility;
    }
    public void setVisibilita(Integer visibilita) {
        this.visibility = visibilita;
    }

    public String getIcona() {
        return icon;
    }
    public void setIcona(String icona) {
        this.icon = icona;
    }

    public List<Weather> getPrevisioni() {
        return forecast;
    }
    public void setPrevisioni(List<Weather> forecast) {
        this.forecast = forecast;
    }

    public String getData() {
        return date;
    }
    public void setData(String data) {
        this.date = data;
    }

    private String getEmoji(String codiceIcona) {
        if (codiceIcona == null) return "☁️";

        String codice = codiceIcona.substring(0, 2); //prende solo i primi 2 caratteri (es. "03" da "03d")

        switch (codice) {
            case "01": return "☀️"; //cielo sereno
            case "02": return "⛅"; //poche nuvole
            case "03": return "☁️"; //nubi sparse
            case "04": return "☁️"; //nuvoloso
            case "09": return "🌧️"; //pioggia leggera
            case "10": return "🌦️"; //pioggia
            case "11": return "⛈️"; //temporale
            case "13": return "❄️"; //neve
            case "50": return "🌫️"; //nebbia
            default: return "☁️";
        }
    }

    @Override
    public String toString() {
        StringBuilder info = new StringBuilder();
        if (date == null) {
            String emoji = getEmoji(icon);
            info.append(emoji).append(" Meteo per ").append(city != null ? city : "N/A").append("\n\n");
            info.append(emoji).append(" ").append(description != null ? description.substring(0, 1).toUpperCase() + description.substring(1) : "N/A").append("\n");
            info.append("🌡️ Temperatura: ").append(temperature != null ? String.format("%.1f", temperature) : "N/A").append("°C\n");
            info.append("🤔 Percepita: ").append(feelsLike != null ? String.format("%.1f", feelsLike) : "N/A").append("°C\n");
            info.append("💨 Vento: ").append(windSpeed != null ? String.format("%.1f", windSpeed) : "N/A").append(" m/s\n");
            info.append("💧 Umidità: ").append(humidity != null ? humidity : "N/A").append("%\n");
            info.append("📊 Pressione: ").append(pressure != null ? pressure : "N/A").append(" hPa\n");
            if (visibility != null)
                info.append("👁️ Visibilità: ").append(visibility / 1000).append(" km\n");

            //previsioni future
            if (forecast != null && !forecast.isEmpty()) {
                info.append("\n📅 Previsioni per i prossimi 5 giorni:\n");
                for (Weather giorno : forecast) { //per ogni giorno
                    if (giorno.getData() != null) {
                        String giornoStr = giorno.getData().substring(0, 10); //YYYY-MM-DD
                        String giornoEmoji = getEmoji(giorno.getIcona());
                        info.append("\n").append(giornoStr).append(":\n");
                        info.append(giornoEmoji).append(" ").append(giorno.getDescrizione() != null ? giorno.getDescrizione() : "N/A").append("\n");
                        info.append("🌡️ ").append(giorno.getTemperatura() != null ? String.format("%.1f", giorno.getTemperatura()) : "N/A").append("°C\n");
                    }
                }
            }
        } else {
            //previsione singola
            String emoji = getEmoji(icon);
            info.append(emoji).append(" ").append(description != null ? description : "N/A").append("\n");
            info.append("🌡️ Temperatura: ").append(temperature != null ? String.format("%.1f", temperature) : "N/A").append("°C\n");
        }
        return info.toString();
    }
}
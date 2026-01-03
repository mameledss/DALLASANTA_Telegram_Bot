package com.flightbot.models;

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

    @Override
    public String toString() {
        StringBuilder info = new StringBuilder();
        info.append("🌤️ Meteo per ").append(city != null ? city : "N/A").append("\n\n");
        info.append("☁️ ").append(description != null ? description : "N/A").append("\n");
        info.append("🌡️ Temperatura: ").append(temperature != null ? String.format("%.1f", temperature) : "N/A").append("°C\n");
        info.append("🤔 Percepita: ").append(feelsLike != null ? String.format("%.1f", feelsLike) : "N/A").append("°C\n");
        info.append("💨 Vento: ").append(windSpeed != null ? String.format("%.1f", windSpeed) : "N/A").append(" m/s\n");
        info.append("💧 Umidità: ").append(humidity != null ? humidity : "N/A").append("%\n");
        info.append("📊 Pressione: ").append(pressure != null ? pressure : "N/A").append(" hPa\n");
        if (visibility != null)
            info.append("👁️ Visibilità: ").append(visibility / 1000).append(" km\n");

        return info.toString();
    }
}
package com.flightbot.models;

public class Airport {
    private String name;
    private String iataCode;
    private String icaoCode;
    private String city;
    private String country;
    private Double latitude;
    private Double longitude;
    private String timezone;
    private String website;
    private String googleMaps;

    public Airport() {}
    public String getNome() {
        return name;
    }
    public void setNome(String nome) {
        this.name = nome;
    }

    public String getCodiceIata() {
        return iataCode;
    }
    public void setCodiceIata(String codiceIata) {
        this.iataCode = codiceIata;
    }

    public String getCodiceIcao() {
        return icaoCode;
    }
    public void setCodiceIcao(String codiceIcao) {
        this.icaoCode = codiceIcao;
    }

    public String getCitta() {
        return city;
    }
    public void setCitta(String citta) {
        this.city = citta;
    }

    public String getPaese() {
        return country;
    }
    public void setPaese(String paese) {
        this.country = paese;
    }

    public Double getLatitudine() {
        return latitude;
    }
    public void setLatitudine(Double latitudine) {
        this.latitude = latitudine;
    }

    public Double getLongitudine() {
        return longitude;
    }
    public void setLongitudine(Double longitudine) {
        this.longitude = longitudine;
    }

    public String getFuso() {
        return timezone;
    }
    public void setFuso(String fuso) {
        this.timezone = fuso;
    }

    public String getSito() {
        return website;
    }
    public void setSito(String sito) {
        this.website = sito;
    }

    public String getGoogleMaps() {
        return googleMaps;
    }
    public void setGoogleMaps(String googleMaps) {
        this.googleMaps = googleMaps;
    }

    @Override
    public String toString() {
        StringBuilder info = new StringBuilder();
        info.append("🏢 ").append(name != null ? name : "N/A").append("\n\n");
        info.append("📍 Codice IATA: ").append(iataCode != null ? iataCode : "N/A").append("\n");
        info.append("📍 Codice ICAO: ").append(icaoCode != null ? icaoCode : "N/A").append("\n");
        info.append("🏙️ Città: ").append(city != null ? city : "N/A").append("\n");
        info.append("🌍 Paese: ").append(country != null ? country : "N/A").append("\n");

        if (website != null)
            info.append("🌐 Sito web: ").append(website).append("\n");
        if (googleMaps != null)
            info.append("🗺️ Google Maps: ").append(googleMaps).append("\n");
        if (latitude != null && longitude != null) {
            info.append("📍 Coordinate: ").append(latitude).append(", ").append(longitude).append("\n");
        }
        return info.toString();
    }
}

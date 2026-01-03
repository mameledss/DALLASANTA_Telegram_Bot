package com.flightbot.models;

public class Ticket {
    private String prezzoTotale;
    private String valuta;
    private Integer numeroScali;
    private String durataTotale;
    private String orarioPartenza;
    private String orarioArrivo;
    private String codiceCompagnia;
    private String numeroVolo;
    private String idOfferta;
    private Integer postiDisponibili;

    public Ticket() {}

    // Getters and Setters
    public String getPrezzoTotale() {
        return prezzoTotale;
    }
    public void setPrezzoTotale(String prezzoTotale) {
        this.prezzoTotale = prezzoTotale;
    }

    public String getValuta() {
        return valuta;
    }
    public void setValuta(String valuta) {
        this.valuta = valuta;
    }

    public Integer getNumeroScali() {
        return numeroScali;
    }
    public void setNumeroScali(Integer numeroScali) {
        this.numeroScali = numeroScali;
    }

    public String getDurataTotale() {
        return durataTotale;
    }
    public void setDurataTotale(String durataTotale) {
        this.durataTotale = durataTotale;
    }

    public String getOrarioPartenza() {
        return orarioPartenza;
    }
    public void setOrarioPartenza(String orarioPartenza) {
        this.orarioPartenza = orarioPartenza;
    }

    public String getOrarioArrivo() {
        return orarioArrivo;
    }
    public void setOrarioArrivo(String orarioArrivo) {
        this.orarioArrivo = orarioArrivo;
    }

    public String getCodiceCompagnia() {
        return codiceCompagnia;
    }
    public void setCodiceCompagnia(String codiceCompagnia) {
        this.codiceCompagnia = codiceCompagnia;
    }

    public String getNumeroVolo() {
        return numeroVolo;
    }
    public void setNumeroVolo(String numeroVolo) {
        this.numeroVolo = numeroVolo;
    }

    public String getIdOfferta() {
        return idOfferta;
    }
    public void setIdOfferta(String idOfferta) {
        this.idOfferta = idOfferta;
    }

    public Integer getPostiDisponibili() {
        return postiDisponibili;
    }
    public void setPostiDisponibili(Integer postiDisponibili) {
        this.postiDisponibili = postiDisponibili;
    }

    @Override
    public String toString() {
        StringBuilder info = new StringBuilder();
        info.append("💰 Prezzo: ").append(prezzoTotale != null ? prezzoTotale : "N/A").append(" ").append(valuta != null ? valuta : "N/A").append("\n");
        info.append("✈️ Scali: ").append(numeroScali != null ? (numeroScali == 0 ? "Diretto" : numeroScali + " scalo/i") : "N/A").append("\n");
        info.append("⏱️ Durata: ").append(durataTotale != null ? formattaDurata(durataTotale) : "N/A").append("\n");
        info.append("🛫 Partenza: ").append(orarioPartenza != null ? formattaDataOra(orarioPartenza) : "N/A").append("\n");
        info.append("🛬 Arrivo: ").append(orarioArrivo != null ? formattaDataOra(orarioArrivo) : "N/A").append("\n");
        info.append("🏢 Volo: ").append(codiceCompagnia != null ? codiceCompagnia : "").append(numeroVolo != null ? numeroVolo : "").append("\n");

        if (idOfferta != null)
            info.append("🔗 ID Offerta: ").append(idOfferta).append("\n");
        if (postiDisponibili != null)
            info.append("💺 Posti disponibili: ").append(postiDisponibili).append("\n");

        return info.toString();
    }

    private String formattaDataOra(String isoDataOra) { //formatta data/ora ISO 8601 in formato leggibile. 2024-01-15T10:30:00 -> 15/01/2024 10:30
        try {
            String[] parti = isoDataOra.split("T"); //divide in parti in base a lettera "T"
            String parteData = parti[0]; //la parte della data è a sinistra
            String parteOra = parti[1].substring(0, 5); //tiene solo HH:MM
            String[] partiData = parteData.split("-"); //divide le parti della data in base a "-"
            return partiData[2] + "/" + partiData[1] + "/" + partiData[0] + " " + parteOra;
        } catch (Exception e) {
            return isoDataOra;
        }
    }

    private String formattaDurata(String isoDuration) { //formatta durata ISO 8601 in formato leggibile. Es: PT2H30M -> 2h 30m
        try {
            String durata = isoDuration.replace("PT", ""); //rimuove "PT"
            String risultato = "";

            if (durata.contains("H")) { //se contiene "H"
                String[] parti = durata.split("H"); //separa in parti
                risultato += parti[0] + "h ";
                durata = parti.length > 1 ? parti[1] : ""; //se c'è meno di una parte al posto dei minuti non mette nulla
            }
            if (durata.contains("M")) {
                String[] parts = durata.split("M");
                risultato += parts[0] + "m";
            }
            return risultato.trim();
        } catch (Exception e) {
            return isoDuration;
        }
    }
}
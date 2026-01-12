package com.flightbot.models;

public class Flight {
    private String flightNumber;
    private String airline;
    private String airlineIata;
    private String status;
    private String departureAirport;
    private String departureIata;
    private String arrivalAirport;
    private String arrivalIata;
    private String departureTime;
    private String arrivalTime;
    private String scheduledDeparture;
    private String scheduledArrival;
    private String aircraftRegistration;
    private String aircraftType;
    private String icao24;
    private Integer delay;
    private Double latitude;
    private Double longitude;
    private Integer altitude;
    private Integer speed;
    private String terminal;
    private String gate;
    private String flightDate;
    private String departureIcao;
    private String arrivalIcao;
    private String arrivalTerminal;
    private String baggageClaim;
    private Integer arrivalDelay;

    public Flight() {}

    public String getNumeroVolo() {
        return flightNumber;
    }
    public void setNumeroVolo(String numeroVolo) {
        this.flightNumber = numeroVolo;
    }

    public String getCopagnia() {
        return airline;
    }
    public void setCompagnia(String compagnia) { this.airline = compagnia; }

    public String getIataCompagnia() {
        return airlineIata;
    }
    public void setIataCompagnia(String iataCompagnia) {
        this.airlineIata = iataCompagnia;
    }

    public String getStato() {
        return status;
    }
    public void setStato(String stato) {
        this.status = stato;
    }

    public String getAeroportoPartenza() {
        return departureAirport;
    }
    public void setAeroportoPartenza(String aeroportoPartenza) {
        this.departureAirport = aeroportoPartenza;
    }

    public String getIataPartenza() {
        return departureIata;
    }
    public void setIataPartenza(String iataPartenza) {
        this.departureIata = iataPartenza;
    }

    public String getAeroportoArrivo() {
        return arrivalAirport;
    }
    public void setAeroportoArrivo(String aeroportoArrivo) {
        this.arrivalAirport = aeroportoArrivo;
    }

    public String getIataArrivo() {
        return arrivalIata;
    }
    public void setIataArrivo(String IataArrivo) {
        this.arrivalIata = IataArrivo;
    }

    public String getOrarioPartenza() {
        return departureTime;
    }
    public void setOrarioPartenza(String orarioPartenza) {
        this.departureTime = orarioPartenza;
    }

    public String getOrarioArrivo() {
        return arrivalTime;
    }
    public void setOrarioArrivo(String orarioArrivo) {
        this.arrivalTime = orarioArrivo;
    }

    public String getPartenzaProgrammata() {
        return scheduledDeparture;
    }
    public void setPartenzaProgrammata(String partenzaProgrammata) {
        this.scheduledDeparture = partenzaProgrammata;
    }

    public String getArrivoProgrammato() {
        return scheduledArrival;
    }
    public void setArrivoProgrammato(String arrivoProgrammato) {
        this.scheduledArrival = arrivoProgrammato;
    }

    public String getRegistrazioneAereo() {
        return aircraftRegistration;
    }
    public void setRegistrazioneAereo(String regitrazioneAereo) { this.aircraftRegistration = regitrazioneAereo; }

    public String getTipoAereo() {
        return aircraftType;
    }
    public void setTipoAereo(String tipoAereo) {
        this.aircraftType = tipoAereo;
    }

    public String getIcao24() {
        return icao24;
    }
    public void setIcao24(String icao24) {
        this.icao24 = icao24;
    }

    public Integer getRitardo() {
        return delay;
    }
    public void setRitardo(Integer ritardo) {
        this.delay = ritardo;
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

    public Integer getAltitudine() {
        return altitude;
    }
    public void setAltitudine(Integer altitudine) {
        this.altitude = altitudine;
    }

    public Integer getVelocita() {
        return speed;
    }
    public void setVelocita(Integer velocita) {
        this.speed = velocita;
    }

    public String getTerminal() { return terminal; }
    public void setTerminal(String terminal) { this.terminal = terminal; }

    public String getGate() {
        return gate;
    }
    public void setGate(String gate) {
        this.gate = gate;
    }

    public String getDataVolo() {
        return flightDate;
    }
    public void setDataVolo(String dataVolo) {
        this.flightDate = dataVolo;
    }

    public String getIcaoPartenza() {
        return departureIcao;
    }
    public void setIcaoPartenza(String icaoPartenza) {
        this.departureIcao = icaoPartenza;
    }

    public String getIcaoArrivo() {
        return arrivalIcao;
    }
    public void setIcaoArrivo(String icaoArrivo) {
        this.arrivalIcao = icaoArrivo;
    }

    public String getTerminalArrivo() {
        return arrivalTerminal;
    }
    public void setTerminalArrivo(String terminalArrivo) {
        this.arrivalTerminal = terminalArrivo;
    }

    public String getRitiroBagagli() {
        return baggageClaim;
    }
    public void setRitiroBagagli(String ritiroBagagli) {
        this.baggageClaim = ritiroBagagli;
    }

    public Integer getRitardoArrivo() {
        return arrivalDelay;
    }
    public void setRitardoArrivo(Integer ritardoArrivo) {
        this.arrivalDelay = ritardoArrivo;
    }

    @Override
    public String toString() {
        StringBuilder info = new StringBuilder();
        info.append("✈️ Volo: ").append(flightNumber != null ? "`" + flightNumber + "`" : "N/A").append("\n");
        info.append("🏢 Compagnia: ").append(airline != null ? airline : "N/A").append("\n");
        info.append("📊 Stato: ").append(status != null ? status.substring(0, 1).toUpperCase() + status.substring(1) : "N/A").append("\n");
        if (flightDate != null)
            info.append("📅 Data: ").append(flightDate).append("\n");
        info.append("\n");

        info.append("🛫 Partenza:\n");
        info.append("-🏢 Aeroporto: ").append(departureAirport != null ? departureAirport : "N/A");
        if (departureIata != null)
            info.append(" (`").append(departureIata).append("`)");
        info.append("\n");
        if (departureIcao != null)
            info.append("-📑 ICAO: ").append("`").append(departureIcao).append("`").append("\n");
        info.append("-🕝 Orario: ").append(departureTime != null ? formattaDataOra(departureTime) : "N/A").append("\n");
        if (gate != null)
            info.append("-🛂 Gate: ").append(gate).append("\n");
        if (delay != null && delay > 0)
            info.append("-⚠️ Ritardo Partenza: ").append(delay).append(" minuti\n");

        info.append("\n🛬 Arrivo:\n");
        info.append("-🏢 Aeroporto: ").append(arrivalAirport != null ? arrivalAirport : "N/A");
        if (arrivalIata != null)
            info.append(" (`").append(arrivalIata).append("`)");
        info.append("\n");
        if (arrivalIcao != null)
            info.append("-📑 ICAO: ").append("`").append(arrivalIcao).append("`").append("\n");
        info.append("-🕝 Orario: ").append(arrivalTime != null ? formattaDataOra(arrivalTime) : "N/A").append("\n");
        if (arrivalTerminal != null)
            info.append("-🏢 Terminal: ").append(arrivalTerminal).append("\n");
        if (baggageClaim != null)
            info.append("-🛄 Bagagli: ").append(baggageClaim).append("\n");
        if (arrivalDelay != null && arrivalDelay > 0)
            info.append("-⚠️ Ritardo Arrivo: ").append(arrivalDelay).append(" minuti\n");

        if (aircraftType != null)
            info.append("\n✈️ Aeromobile: ").append(aircraftType).append("\n");

        if (icao24 != null) //ICAO24->id 24 bit per aerei
            info.append("\n📡 ICAO24: ").append("`").append(icao24).append("`").append("\n");

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
}

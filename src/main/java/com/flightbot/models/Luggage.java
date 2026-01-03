package com.flightbot.models;

public class Luggage {
    private String airline;
    private String maxDimensions;
    private String maxWeight;

    public Luggage(String compagnia, String maxDim, String maxPeso) {
        this.airline = compagnia;
        this.maxDimensions = maxDim;
        this.maxWeight = maxPeso;
    }

    public String getCompagnia() { return airline; }
    public void setCompagnia(String compagnia) { this.airline = compagnia; }

    public String getMaxDim() { return maxDimensions; }
    public void setMaxDim(String maxDim) { this.maxDimensions = maxDim; }

    public String getMaxPeso() { return maxWeight; }
    public void setMaxPeso(String maxPeso) { this.maxWeight = maxPeso; }

    @Override
    public String toString() {
        return "✈️ Compagnia: " + airline + "\n📏 Dimensioni massime: " + maxDimensions + "\n⚖️ Peso massimo: " + maxWeight;
    }
}
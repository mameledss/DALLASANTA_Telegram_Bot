package com.flightbot.models;

import java.sql.Timestamp;

public class UserProfile {
    private final long chatId;
    private final String username;
    private final String firstName;
    private final String lastName;
    private final Timestamp createdAt;
    private final Timestamp lastSeen;

    public UserProfile(long chatId, String username, String nome, String cognome, Timestamp creatoA, Timestamp ultimoAccesso) {
        this.chatId = chatId;
        this.username = username;
        this.firstName = nome;
        this.lastName = cognome;
        this.createdAt = creatoA;
        this.lastSeen = ultimoAccesso;
    }

    public long getChatId() { return chatId; }

    public String getUsername() {
        return username;
    }

    public String getNome() {
        return firstName;
    }

    public String getCognome() {
        return lastName;
    }

    public Timestamp getCreatoA() {
        return createdAt;
    }

    public Timestamp getUltimoAccesso() {
        return lastSeen;
    }
}
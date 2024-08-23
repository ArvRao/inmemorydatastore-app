// src/main/java/com/example/inmemorystore/model/KeyValuePair.java

package com.inmemorydb.inmemorydb_app.model;

import java.io.Serializable;
import java.time.Instant;

public class KeyValuePair implements Serializable {
    private static final long serialVersionUID = 1L;
    private String key;
    private String value;
    private Instant expirationDate;
    private Instant insertTimestamp;

    public KeyValuePair(String key, String value, Instant expirationDate, Instant insertTimestamp) {
        this.key = key;
        this.value = value;
        this.expirationDate = expirationDate;
        this.insertTimestamp = insertTimestamp;
    }

    // Getters and setters
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public Instant getExpirationDate() { return expirationDate; }
    public void setExpirationDate(Instant expirationDate) { this.expirationDate = expirationDate; }
    public Instant getInsertTimestamp() { return insertTimestamp; }
    public void setInsertTimestamp(Instant insertTimestamp) { this.insertTimestamp = insertTimestamp; }
    public boolean isExpired() { return Instant.now().isAfter(expirationDate); }
}
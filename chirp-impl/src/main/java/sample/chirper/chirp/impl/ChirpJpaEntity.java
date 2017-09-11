package sample.chirper.chirp.impl;

import sample.chirper.chirp.api.Chirp;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;
import java.time.Instant;

@Entity
@Table(name = "chirp")
class ChirpJpaEntity {
    private String userId;
    private String message;
    private Instant timestamp;
    private String uuid;

    static ChirpJpaEntity from(Chirp chirp) {
        ChirpJpaEntity entity = new ChirpJpaEntity();
        entity.setUserId(chirp.userId);
        entity.setMessage(chirp.message);
        entity.setTimestamp(chirp.timestamp);
        entity.setUuid(chirp.uuid);
        return entity;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Id
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}

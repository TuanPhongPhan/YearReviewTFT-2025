package com.tuanphong.yearreviewtft.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "match_cache")
public class MatchCacheEntity {

    @Id
    @Column(length = 64)
    private String matchId;

    @Column(nullable = false, length = 16)
    private String routing; // "EUROPE"

    @Column(nullable = false)
    private Instant fetchedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", columnDefinition = "jsonb", nullable = false)
    private String payloadJson;

    @PrePersist
    void prePersist() {
        fetchedAt = Instant.now();
        if (routing == null) routing = "EUROPE";
    }

    // getters/setters

    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }
    public String getRouting() { return routing; }
    public void setRouting(String routing) { this.routing = routing; }
    public Instant getFetchedAt() { return fetchedAt; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
}

package com.tuanphong.yearreviewtft.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "player_match",
        uniqueConstraints = @UniqueConstraint(columnNames = {"puuid", "year", "match_id"})
)

public class PlayerMatchEntity {

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String puuid;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false, length = 64)
    private String matchId;

    // getters/setters

    public Long getId() { return id; }
    public String getPuuid() { return puuid; }
    public void setPuuid(String puuid) { this.puuid = puuid; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }
    public Instant getCreatedAt() { return createdAt; }
}

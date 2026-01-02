package com.tuanphong.yearreviewtft.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "player_year",
        uniqueConstraints = @UniqueConstraint(columnNames = {"puuid", "year"}))

public class PlayerYearEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String puuid;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false, length = 16)
    private String platform; // EUW1/EUN1 (for future endpoints)

    @Column(nullable = false, length = 64)
    private String riotId;

    @Column(nullable = false, length = 32)
    private String state; // QUEUED, FETCHING_MATCH_IDS, FETCHING_MATCH_DETAILS, DONE, FAILED

    @Column(nullable = false)
    private int matchIdsFound;

    @Column(nullable = false)
    private int matchesCached;

    @Column(nullable = false)
    private boolean summaryReady;

    @Column(nullable = false, length = 512)
    private String message;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (state == null) state = "QUEUED";
        if (message == null) message = "Queued";
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    // getters/setters (generate in IntelliJ)

    public Long getId() { return id; }
    public String getPuuid() { return puuid; }
    public void setPuuid(String puuid) { this.puuid = puuid; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getRiotId() { return riotId; }
    public void setRiotId(String riotId) { this.riotId = riotId; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public int getMatchIdsFound() { return matchIdsFound; }
    public void setMatchIdsFound(int matchIdsFound) { this.matchIdsFound = matchIdsFound; }
    public int getMatchesCached() { return matchesCached; }
    public void setMatchesCached(int matchesCached) { this.matchesCached = matchesCached; }
    public boolean isSummaryReady() { return summaryReady; }
    public void setSummaryReady(boolean summaryReady) { this.summaryReady = summaryReady; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

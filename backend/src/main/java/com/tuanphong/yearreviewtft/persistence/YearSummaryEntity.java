package com.tuanphong.yearreviewtft.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "year_summary",
        uniqueConstraints = @UniqueConstraint(name = "uk_year_summary", columnNames = {"puuid", "year"})
)
public class YearSummaryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String puuid;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private Instant computedAt;

    @Column(columnDefinition = "text", nullable = false)
    private String summaryJson;

    @PrePersist
    void prePersist() {
        computedAt = Instant.now();
    }

    // getters/setters
    public Long getId() { return id; }
    public String getPuuid() { return puuid; }
    public void setPuuid(String puuid) { this.puuid = puuid; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public Instant getComputedAt() { return computedAt; }
    public String getSummaryJson() { return summaryJson; }
    public void setSummaryJson(String summaryJson) { this.summaryJson = summaryJson; }
}

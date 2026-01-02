package com.tuanphong.yearreviewtft.persistence;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlayerMatchRepository extends JpaRepository<PlayerMatchEntity, Long> {
    List<PlayerMatchEntity> findByPuuidAndYear(String puuid, int year);

    boolean existsByPuuidAndYearAndMatchId(String puuid, int year, String matchId);

    @Query("select pm.matchId from PlayerMatchEntity pm where pm.puuid = :puuid and pm.year = :year")
    List<String> findMatchIdsByPuuidAndYear(
            @Param("puuid") String puuid,
            @Param("year") int year
    );

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO player_match (puuid, year, match_id)
        VALUES (:puuid, :year, :matchId)
        ON CONFLICT (puuid, year, match_id) DO NOTHING
        """, nativeQuery = true)
    void insertIgnoreDuplicate(
            @Param("puuid") String puuid,
            @Param("year") int year,
            @Param("matchId") String matchId
    );
}

package com.tuanphong.yearreviewtft.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface PlayerYearRepository extends JpaRepository<PlayerYearEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Transactional // ensures transaction is active for the lock
    @Query("select p from PlayerYearEntity p where p.puuid = :puuid and p.year = :year")
    Optional<PlayerYearEntity> lockByPuuidAndYear(@Param("puuid") String puuid, @Param("year") int year);

    Optional<PlayerYearEntity> findByPuuidAndYear(String puuid, int year);

    Optional<PlayerYearEntity> findByRiotIdAndYear(String riotId, int year);
}

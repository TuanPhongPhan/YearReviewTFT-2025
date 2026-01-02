package com.tuanphong.yearreviewtft.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface YearSummaryRepository extends JpaRepository<YearSummaryEntity, Long> {
    Optional<YearSummaryEntity> findByPuuidAndYear(String puuid, int year);
}

package com.tuanphong.yearreviewtft.service;

import com.tuanphong.yearreviewtft.api.dto.WrappedRequest;
import com.tuanphong.yearreviewtft.persistence.PlayerYearEntity;
import com.tuanphong.yearreviewtft.persistence.PlayerYearRepository;
import jakarta.persistence.EntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WrappedTxService {

    private final PlayerYearRepository playerYearRepository;
    private final EntityManager entityManager;

    public WrappedTxService(PlayerYearRepository playerYearRepository, EntityManager entityManager) {
        this.playerYearRepository = playerYearRepository;
        this.entityManager = entityManager;
    }

    /**
     * Must run inside a transaction because lockByPuuidAndYear uses SELECT ... FOR NO KEY UPDATE.
     * This method is intentionally small and DB-only.
     */
    @Transactional
    public PlayerYearEntity lockOrCreate(String puuid, int year, WrappedRequest req) {

        PlayerYearEntity py = playerYearRepository.lockByPuuidAndYear(puuid, year).orElse(null);
        if (py != null) return py;

        try {
            py = new PlayerYearEntity();
            py.setPuuid(puuid);
            py.setYear(year);
            py.setPlatform(req.platform());
            py.setRiotId(req.riotId());
            py.setState("QUEUED");
            py.setMessage("Queued");
            py.setMatchIdsFound(0);
            py.setMatchesCached(0);
            py.setSummaryReady(false);

            // save+flush so the row exists immediately and gets an ID
            return playerYearRepository.saveAndFlush(py);

        } catch (DataIntegrityViolationException e) {
            // someone else inserted concurrently -> clear persistence context and lock existing row
            entityManager.clear();
            return playerYearRepository.lockByPuuidAndYear(puuid, year)
                    .orElseThrow(() -> new IllegalStateException("player_year missing after race", e));
        }
    }

    @Transactional
    public void markStarted(String puuid, int year, String riotId, String platform, String state, String message) {
        PlayerYearEntity py = playerYearRepository.lockByPuuidAndYear(puuid, year)
                .orElseThrow(() -> new IllegalStateException("player_year not found"));
        py.setRiotId(riotId);
        py.setPlatform(platform);
        py.setState(state);
        py.setMessage(message);
        playerYearRepository.save(py);
    }
}

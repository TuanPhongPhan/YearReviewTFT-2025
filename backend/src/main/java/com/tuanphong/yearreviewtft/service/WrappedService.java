package com.tuanphong.yearreviewtft.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuanphong.yearreviewtft.api.dto.WrappedRequest;
import com.tuanphong.yearreviewtft.api.dto.WrappedRequestResponse;
import com.tuanphong.yearreviewtft.api.dto.WrappedStatusResponse;
import com.tuanphong.yearreviewtft.persistence.MatchCacheEntity;
import com.tuanphong.yearreviewtft.persistence.MatchCacheRepository;
import com.tuanphong.yearreviewtft.persistence.PlayerMatchEntity;
import com.tuanphong.yearreviewtft.persistence.PlayerMatchRepository;
import com.tuanphong.yearreviewtft.persistence.PlayerYearEntity;
import com.tuanphong.yearreviewtft.persistence.PlayerYearRepository;
import com.tuanphong.yearreviewtft.riot.TftMatchService;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class WrappedService {

    private final TftMatchService tftMatchService;
    private final PlayerYearRepository playerYearRepository;
    private final PlayerMatchRepository playerMatchRepository;
    private final MatchCacheRepository matchCacheRepository;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;
    private final WrappedSummaryService wrappedSummaryService;
    private final WrappedTxService wrappedTxService;


    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    public WrappedService(
            TftMatchService tftMatchService,
            PlayerYearRepository playerYearRepository,
            PlayerMatchRepository playerMatchRepository,
            MatchCacheRepository matchCacheRepository,
            ObjectMapper objectMapper,
            EntityManager entityManager,
            WrappedSummaryService wrappedSummaryService, WrappedTxService wrappedTxService
    ) {
        this.tftMatchService = tftMatchService;
        this.playerYearRepository = playerYearRepository;
        this.playerMatchRepository = playerMatchRepository;
        this.matchCacheRepository = matchCacheRepository;
        this.objectMapper = objectMapper;
        this.wrappedSummaryService = wrappedSummaryService;
        this.entityManager = entityManager;
        this.wrappedTxService = wrappedTxService;
    }

    public WrappedRequestResponse start(WrappedRequest req) {

        // 0) Fast path by riotId+year (no Riot call)
        var existingByRiotId = playerYearRepository.findByRiotIdAndYear(req.riotId(), req.year());
        if (existingByRiotId.isPresent()) {
            PlayerYearEntity py = existingByRiotId.get();

            if (py.isSummaryReady() || "DONE".equals(py.getState())) {
                return new WrappedRequestResponse(py.getPuuid(), req.year(), null, "DONE");
            }

            // IMPORTANT FIX:
            // If state is QUEUED, that does NOT mean a job is running.
            // We allow restart instead of returning QUEUED forever.
            if (isRunning(py.getState())) {
                return new WrappedRequestResponse(py.getPuuid(), req.year(), null, py.getState());
            }
            // if FAILED / QUEUED -> continue to restart below
        }

        // 1) Resolve puuid only when needed
        String puuid = tftMatchService.resolvePuuid(req.riotId()).block();
        if (puuid == null || puuid.isBlank()) throw new IllegalStateException("Could not resolve PUUID.");

        // 2) If summary exists, no job needed
        if (wrappedSummaryService.load(puuid, req.year()).isPresent()) {
            return new WrappedRequestResponse(puuid, req.year(), null, "DONE");
        }

        // 3) Acquire DB lock for this (puuid, year)
        PlayerYearEntity py = wrappedTxService.lockOrCreate(puuid, req.year(), req);

        // If row doesn't exist yet, create it safely (race-proof with unique constraint)
        if (py == null) {
            try {
                py = new PlayerYearEntity();
                py.setPuuid(puuid);
                py.setYear(req.year());
                py.setPlatform(req.platform());
                py.setRiotId(req.riotId());
                py.setState("QUEUED");
                py.setMessage("Queued");
                py.setMatchIdsFound(0);
                py.setMatchesCached(0);
                py.setSummaryReady(false);

                // flush so the row definitely exists inside this tx
                py = playerYearRepository.saveAndFlush(py);

                // lock again (ensures we hold the lock even after insert)
                py = playerYearRepository.lockByPuuidAndYear(puuid, req.year())
                        .orElseThrow(() -> new IllegalStateException("player_year missing after insert"));
            } catch (DataIntegrityViolationException e) {
                entityManager.clear();

                // someone else inserted simultaneously -> lock & use that row
                py = playerYearRepository.lockByPuuidAndYear(puuid, req.year())
                        .orElseThrow(() -> new IllegalStateException("player_year missing after race"));
            }
        }

        // 4) After locking: decide what to do
        if (py.isSummaryReady() || "DONE".equals(py.getState())) {
            return new WrappedRequestResponse(puuid, req.year(), null, "DONE");
        }

        // IMPORTANT FIX:
        // QUEUED should not block starting. Only block if truly running states.
        if (isRunning(py.getState())) {
            return new WrappedRequestResponse(puuid, req.year(), null, py.getState());
        }

        // 5) Start job: set running state while still holding lock
        String jobId = UUID.randomUUID().toString();

        py.setPlatform(req.platform());
        py.setRiotId(req.riotId());
        py.setState("FETCHING_MATCH_IDS");
        py.setMessage("Starting...");
        py.setMatchIdsFound(0);
        py.setMatchesCached(0);
        py.setSummaryReady(false);
        playerYearRepository.save(py);

        // Transaction ends after method returns; the row state prevents double-starts
        executor.submit(() -> runJob(req, puuid));

        return new WrappedRequestResponse(puuid, req.year(), jobId, py.getState());
    }

    /**
     * Running states that SHOULD block starting another job.
     * NOTE: QUEUED is intentionally NOT considered "running" here.
     */
    private static boolean isRunning(String state) {
        if (state == null) return false;
        return state.equals("FETCHING_MATCH_IDS")
                || state.equals("SAVING_MATCH_IDS")
                || state.equals("FETCHING_MATCH_DETAILS")
                || state.equals("COMPUTING");
    }

    public WrappedStatusResponse status(String puuid, int year) {
        PlayerYearEntity py = playerYearRepository.findByPuuidAndYear(puuid, year).orElse(null);
        if (py == null) {
            return new WrappedStatusResponse(puuid, year, "NOT_FOUND", 0, 0, false,
                    "No run found. Call /api/wrapped/request.");
        }
        return new WrappedStatusResponse(
                puuid,
                year,
                py.getState(),
                py.getMatchIdsFound(),
                py.getMatchesCached(),
                py.isSummaryReady(),
                py.getMessage()
        );
    }

    /**
     * Official output endpoint: returns the saved summary JSON (year_summary.summary_json).
     * If not ready, returns a small info payload.
     */
    public Map<String, Object> getSummary(String puuid, int year) {
        return wrappedSummaryService.load(puuid, year)
                .orElse(Map.of(
                        "ready", false,
                        "message", "Summary not ready yet. Poll /api/wrapped/status.",
                        "puuid", puuid,
                        "year", year
                ));
    }

    // ---------------- internal ----------------

    private void runJob(WrappedRequest req, String puuid) {
        PlayerYearEntity py = playerYearRepository.findByPuuidAndYear(puuid, req.year())
                .orElseThrow(() -> new IllegalStateException("player_year missing"));

        try {
            update(py, "FETCHING_MATCH_IDS", "Fetching match IDs...");

            List<String> matchIds = playerMatchRepository.findMatchIdsByPuuidAndYear(puuid, req.year());

            if (matchIds == null || matchIds.isEmpty()) {
                matchIds = tftMatchService.fetchAllMatchIdsForYear(puuid, req.year()).block();
                if (matchIds == null) matchIds = List.of();
            }

            py.setMatchIdsFound(matchIds.size());
            update(py, "SAVING_MATCH_IDS", "Saving match IDs...");

            // store match ids (dedup thanks to unique constraint; we also check exists to reduce noise)
            for (String matchId : matchIds) {
                playerMatchRepository.insertIgnoreDuplicate(puuid, req.year(), matchId);
            }


            update(py, "FETCHING_MATCH_DETAILS", "Caching match JSON...");

            int cachedNow = 0;
            for (String matchId : matchIds) {
                if (matchCacheRepository.existsById(matchId)) {
                    continue; // already cached
                }

                JsonNode matchJson;

                while (true) {
                    try {
                        matchJson = tftMatchService.fetchMatchDetail(matchId).block();
                        break; // success
                    } catch (RuntimeException ex) {
                        String msg = ex.getMessage() == null ? "" : ex.getMessage();

                        if (msg.contains("Riot rate limited")) {
                            long waitSeconds = extractRetryAfterSeconds(msg, 4L);

                            update(py, "FETCHING_MATCH_DETAILS",
                                    "Rate limited. Waiting " + waitSeconds + "s... Cached "
                                            + py.getMatchesCached() + "/" + matchIds.size());

                            try {
                                Thread.sleep(waitSeconds * 1000L);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                throw new IllegalStateException("Job interrupted while rate-limited");
                            }
                            continue; // retry same match
                        }

                        throw ex; // real error
                    }
                }

                if (matchJson == null) continue;

                MatchCacheEntity cache = new MatchCacheEntity();
                cache.setMatchId(matchId);
                cache.setRouting("EUROPE");
                cache.setPayloadJson(objectMapper.writeValueAsString(matchJson));
                matchCacheRepository.save(cache);

                cachedNow++;
                py.setMatchesCached(py.getMatchesCached() + 1);

                if (cachedNow % 10 == 0) {
                    update(py, "FETCHING_MATCH_DETAILS",
                            "Cached " + py.getMatchesCached() + "/" + matchIds.size() + " matches...");
                }
            }

            update(py, "COMPUTING", "Computing year summary...");
            wrappedSummaryService.computeAndSave(puuid, req.year());

            py.setSummaryReady(true);
            update(py, "DONE", "Summary ready.");

        } catch (Exception e) {
            update(py, "FAILED", "Failed: " + e.getMessage());
        }
    }

    private void update(PlayerYearEntity py, String state, String msg) {
        py.setState(state);
        py.setMessage(msg);
        playerYearRepository.save(py);
    }

    private static long extractRetryAfterSeconds(String message, long fallback) {
        // Expected message: "Riot rate limited. Retry after 4s"
        try {
            int idx = message.lastIndexOf("after");
            if (idx == -1) return fallback;
            String digits = message.substring(idx).replaceAll("[^0-9]", "");
            if (digits.isBlank()) return fallback;
            return Long.parseLong(digits);
        } catch (Exception e) {
            return fallback;
        }
    }

    public Map<String, Object> recomputeSummary(String puuid, int year) {
        return wrappedSummaryService.computeAndSave(puuid, year);
    }
}

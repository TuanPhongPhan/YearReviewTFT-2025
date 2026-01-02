package com.tuanphong.yearreviewtft.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuanphong.yearreviewtft.persistence.MatchCacheEntity;
import com.tuanphong.yearreviewtft.persistence.MatchCacheRepository;
import com.tuanphong.yearreviewtft.persistence.PlayerMatchEntity;
import com.tuanphong.yearreviewtft.persistence.PlayerMatchRepository;
import com.tuanphong.yearreviewtft.persistence.YearSummaryEntity;
import com.tuanphong.yearreviewtft.persistence.YearSummaryRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class WrappedSummaryService {

    private final PlayerMatchRepository playerMatchRepository;
    private final MatchCacheRepository matchCacheRepository;
    private final YearSummaryRepository yearSummaryRepository;
    private final ObjectMapper objectMapper;

    public WrappedSummaryService(
            PlayerMatchRepository playerMatchRepository,
            MatchCacheRepository matchCacheRepository,
            YearSummaryRepository yearSummaryRepository,
            ObjectMapper objectMapper
    ) {
        this.playerMatchRepository = playerMatchRepository;
        this.matchCacheRepository = matchCacheRepository;
        this.yearSummaryRepository = yearSummaryRepository;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> computeAndSave(String puuid, int year) {
        List<PlayerMatchEntity> pms = playerMatchRepository.findByPuuidAndYear(puuid, year);
        List<String> matchIds = pms.stream().map(PlayerMatchEntity::getMatchId).toList();

        int gamesPlayed = 0;
        long placementSum = 0;

        int[] placementHist = new int[9]; // 1..8
        int top4Count = 0;

        Map<String, Integer> traitCounts = new HashMap<>();
        Map<String, Integer> augmentCounts = new HashMap<>();
        Map<String, Integer> unitCounts = new HashMap<>();

        BestWorst best = null;
        BestWorst worst = null;

        for (String matchId : matchIds) {
            Optional<MatchCacheEntity> cacheOpt = matchCacheRepository.findById(matchId);
            if (cacheOpt.isEmpty()) continue;

            JsonNode root = readJson(cacheOpt.get().getPayloadJson());
            if (root == null) continue;

            JsonNode participants = root.path("info").path("participants");
            if (!participants.isArray()) continue;

            JsonNode me = findMe(participants, puuid);
            if (me == null) continue;

            int placement = me.path("placement").asInt(-1);
            if (placement < 1 || placement > 8) continue;

            gamesPlayed++;
            placementSum += placement;
            placementHist[placement]++;

            if (placement <= 4) top4Count++;

            // Track best/worst game
            int level = me.path("level").asInt(0);
            long goldLeft = me.path("gold_left").asLong(0);
            BestWorst current = new BestWorst(matchId, placement, level, goldLeft);

            best = pickBetter(best, current);
            worst = pickWorse(worst, current);

            // Traits (count only “active” traits to avoid noise)
            JsonNode traits = me.path("traits");
            if (traits.isArray()) {
                for (JsonNode t : traits) {
                    String name = t.path("name").asText(null);
                    if (name == null || name.isBlank()) continue;

                    int tierCurrent = t.path("tier_current").asInt(0);
                    int numUnits = t.path("num_units").asInt(0);

                    // heuristic: trait is active if tier_current > 0 OR num_units > 0
                    if (tierCurrent > 0 || numUnits > 0) {
                        traitCounts.merge(name, 1, Integer::sum);
                    }
                }
            }

            // Augments (field name varies by set, so try multiple)
            // Common: augments (array of strings)
            JsonNode augments = me.path("augments");
            if (augments.isArray()) {
                for (JsonNode a : augments) {
                    if (a.isTextual()) augmentCounts.merge(a.asText(), 1, Integer::sum);
                }
            } else {
                // fallback: "augments" might be nested / different; ignore if not found
            }

            // Units
            JsonNode units = me.path("units");
            if (units.isArray()) {
                for (JsonNode u : units) {
                    String name = u.path("character_id").asText(null);
                    if (name == null || name.isBlank()) continue;
                    unitCounts.merge(name, 1, Integer::sum);
                }
            }
        }

        double avgPlacement = gamesPlayed == 0 ? 0.0 : (double) placementSum / gamesPlayed;
        double top4Rate = gamesPlayed == 0 ? 0.0 : (double) top4Count / gamesPlayed;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("ready", true);
        summary.put("puuid", puuid);
        summary.put("year", year);

        summary.put("gamesPlayed", gamesPlayed);
        summary.put("avgPlacement", round2(avgPlacement));
        summary.put("top4Rate", round2(top4Rate));

        summary.put("placements", placementsMap(placementHist));
        summary.put("topTraits", topN(traitCounts, 10));
        summary.put("topAugments", topN(augmentCounts, 10));
        summary.put("topUnits", topN(unitCounts, 10));

        summary.put("bestGame", best == null ? null : best.toMap());
        summary.put("worstGame", worst == null ? null : worst.toMap());

        upsert(puuid, year, summary);

        return summary;
    }

    public Optional<Map<String, Object>> load(String puuid, int year) {
        return yearSummaryRepository.findByPuuidAndYear(puuid, year).map(ent -> {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = objectMapper.readValue(ent.getSummaryJson(), Map.class);
                return map;
            } catch (Exception e) {
                return Map.of("ready", false, "message", "Failed to parse saved summary JSON");
            }
        });
    }

    // ---------------- helpers ----------------

    private void upsert(String puuid, int year, Map<String, Object> summary) {
        YearSummaryEntity ent = yearSummaryRepository.findByPuuidAndYear(puuid, year)
                .orElseGet(YearSummaryEntity::new);

        ent.setPuuid(puuid);
        ent.setYear(year);

        try {
            ent.setSummaryJson(objectMapper.writeValueAsString(summary));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize summary JSON: " + e.getMessage(), e);
        }

        yearSummaryRepository.save(ent);
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    private static JsonNode findMe(JsonNode participants, String puuid) {
        for (JsonNode p : participants) {
            if (puuid.equals(p.path("puuid").asText())) return p;
        }
        return null;
    }

    private static List<Map<String, Object>> topN(Map<String, Integer> counts, int n) {
        return counts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(n)
                .map(e -> Map.<String, Object>of("name", e.getKey(), "count", e.getValue()))
                .collect(Collectors.toList());
    }

    private static Map<String, Integer> placementsMap(int[] hist) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 1; i <= 8; i++) m.put(String.valueOf(i), hist[i]);
        return m;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    // best = smaller placement; tie-break higher level; tie-break more goldLeft
    private static BestWorst pickBetter(BestWorst best, BestWorst cur) {
        if (best == null) return cur;
        if (cur.placement < best.placement) return cur;
        if (cur.placement > best.placement) return best;
        if (cur.level > best.level) return cur;
        if (cur.level < best.level) return best;
        return (cur.goldLeft > best.goldLeft) ? cur : best;
    }

    // worst = larger placement; tie-break lower level; tie-break less goldLeft
    private static BestWorst pickWorse(BestWorst worst, BestWorst cur) {
        if (worst == null) return cur;
        if (cur.placement > worst.placement) return cur;
        if (cur.placement < worst.placement) return worst;
        if (cur.level < worst.level) return cur;
        if (cur.level > worst.level) return worst;
        return (cur.goldLeft < worst.goldLeft) ? cur : worst;
    }

    private record BestWorst(String matchId, int placement, int level, long goldLeft) {
        Map<String, Object> toMap() {
            return Map.of(
                    "matchId", matchId,
                    "placement", placement,
                    "level", level,
                    "goldLeft", goldLeft
            );
        }
    }
}

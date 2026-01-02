package com.tuanphong.yearreviewtft.riot;

import com.fasterxml.jackson.databind.JsonNode;
import com.tuanphong.yearreviewtft.api.dto.RiotAccountResponse;
import org.apache.tomcat.util.buf.UriUtil;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TftMatchService {

    private static final String EUROPE = "https://europe.api.riotgames.com";

    private final WebClient riotWebClient;

    public TftMatchService(WebClient riotWebClient) {
        this.riotWebClient = riotWebClient;
    }

    // ---------- Public API ----------

    public Mono<String> resolvePuuid(String riotId) {
        RiotIdParts parts = splitAndEncodeRiotId(riotId);

        String url = EUROPE + "/riot/account/v1/accounts/by-riot-id/"
                + parts.gameNameEncoded() + "/" + parts.tagLineEncoded();

        return getJson(url)
                .map(json -> {
                    JsonNode puuid = json.get("puuid");
                    if (puuid == null || puuid.asText().isBlank()) {
                        throw new IllegalStateException("No puuid returned from Riot account endpoint.");
                    }
                    return puuid.asText();
                });
    }

    public Mono<List<String>> fetchAllMatchIdsForYear(String puuid, int year) {
        long[] range = yearRangeEpochSecondsBerlin(year);
        long startTime = range[0];
        long endTime = range[1];

        return Mono.fromSupplier(() -> {
            ArrayList<String> all = new ArrayList<>();
            int start = 0;

            while (true) {
                String url = EUROPE + "/tft/match/v1/matches/by-puuid/" + puuid
                        + "/ids?start=" + start
                        + "&count=100"
                        + "&startTime=" + startTime
                        + "&endTime=" + endTime;

                List<String> page = get(url, new ParameterizedTypeReference<List<String>>() {}).block();

                if (page == null || page.isEmpty()) break;

                all.addAll(page);
                start += 100;

                if (start > 100_000) {
                    throw new IllegalStateException("Paging exceeded 100000 offset. Aborting.");
                }
            }

            return all;
        });
    }

    public Mono<JsonNode> fetchMatchDetail(String matchId) {
        String url = EUROPE + "/tft/match/v1/matches/" + matchId;
        return getJson(url);
    }

    // ---------- Internal HTTP helpers (NO double-consume) ----------

    private Mono<JsonNode> getJson(String url) {
        return get(url, JsonNode.class);
    }

    private <T> Mono<T> get(String url, Class<T> clazz) {
        return riotWebClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(status -> status.value() == 429, resp -> {
                    long retryAfter = resp.headers().header("Retry-After").stream()
                            .findFirst()
                            .map(Long::parseLong)
                            .orElse(2L);
                    return Mono.error(new RiotRateLimitedException(retryAfter));
                })
                .bodyToMono(clazz)
                .retryWhen(retry429());
    }

    private <T> Mono<T> get(String url, ParameterizedTypeReference<T> typeRef) {
        return riotWebClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(status -> status.value() == 429, resp -> {
                    long retryAfter = resp.headers().header("Retry-After").stream()
                            .findFirst()
                            .map(Long::parseLong)
                            .orElse(2L);
                    return Mono.error(new RiotRateLimitedException(retryAfter));
                })
                .bodyToMono(typeRef)
                .retryWhen(retry429());
    }

    private static Retry retry429() {
        return Retry.backoff(6, Duration.ofSeconds(2))
                .filter(ex -> ex instanceof RiotRateLimitedException)
                .transientErrors(true)
                .onRetryExhaustedThrow((spec, signal) -> signal.failure());
    }


    // ---------- Time + riotId utils ----------

    private static long[] yearRangeEpochSecondsBerlin(int year) {
        ZoneId zone = ZoneId.of("Europe/Berlin");
        long start = ZonedDateTime.of(year, 1, 1, 0, 0, 0, 0, zone).toEpochSecond();
        long end = ZonedDateTime.of(year + 1, 1, 1, 0, 0, 0, 0, zone).toEpochSecond();
        return new long[]{start, end};
    }

    private static RiotIdParts splitAndEncodeRiotId(String riotId) {
        String[] parts = riotId.split("#", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new IllegalArgumentException("riotId must be in format GameName#TAG");
        }
        String gameNameEnc = URLEncoder.encode(parts[0], StandardCharsets.UTF_8);
        String tagLineEnc = URLEncoder.encode(parts[1], StandardCharsets.UTF_8);
        return new RiotIdParts(gameNameEnc, tagLineEnc);
    }

    public RiotAccountResponse resolveRiotId(String riotId) {
        String[] parts = riotId.split("#", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid Riot ID format. Expected 'GameName#TAG'.");
        }

        String gameName = parts[0];
        String tagLine = parts[1];

        String url = String.format(
                "https://europe.api.riotgames.com/riot/account/v1/accounts/by-riot-id/%s/%s",
                UriUtils.encode(gameName, StandardCharsets.UTF_8),
                UriUtils.encode(tagLine, StandardCharsets.UTF_8)
        );

        var response = riotWebClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(RiotAccountResponse.class)
                .block();

        if (response == null) {
            throw new IllegalStateException("No response from Riot API.");
        }

        return response;
    }

    private record RiotIdParts(String gameNameEncoded, String tagLineEncoded) {}

    private static class RiotRateLimitedException extends RuntimeException {
        private final long retryAfterSeconds;

        RiotRateLimitedException(long retryAfterSeconds) {
            super("Riot rate limited. Retry after " + retryAfterSeconds + "s");
            this.retryAfterSeconds = retryAfterSeconds;
        }

        long retryAfterSeconds() {
            return retryAfterSeconds;
        }
    }
}

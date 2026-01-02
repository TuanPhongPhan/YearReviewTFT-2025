package com.tuanphong.yearreviewtft.api;

import com.tuanphong.yearreviewtft.api.dto.WrappedRequest;
import com.tuanphong.yearreviewtft.api.dto.WrappedRequestResponse;
import com.tuanphong.yearreviewtft.api.dto.WrappedStatusResponse;
import com.tuanphong.yearreviewtft.service.WrappedService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/wrapped")
public class WrappedController {

    private final WrappedService wrappedService;

    public WrappedController(WrappedService wrappedService) {
        this.wrappedService = wrappedService;
    }

    /**
     * Starts generating (async) the wrapped for a Riot ID + year.
     * Returns immediately with a job id and resolved puuid.
     */
    @PostMapping(value = "/request", produces = MediaType.APPLICATION_JSON_VALUE)
    public WrappedRequestResponse request(@RequestBody @Valid WrappedRequest req) {
        return wrappedService.start(req);
    }

    /**
     * Polling endpoint for frontend while wrapped is generating.
     */
    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public WrappedStatusResponse status(
            @RequestParam @NotBlank String puuid,
            @RequestParam int year
    ) {
        return wrappedService.status(puuid, year);
    }

    /**
     * Returns the computed summary JSON (fast).
     * If not ready, returns 404-ish info payload (or you can throw ResponseStatusException).
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> get(
            @RequestParam @NotBlank String puuid,
            @RequestParam int year
    ) {
        return wrappedService.getSummary(puuid, year);
    }

    @PostMapping(value = "/recompute", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> recompute(@RequestParam String puuid, @RequestParam int year) {
        return wrappedService.recomputeSummary(puuid, year);
    }

}

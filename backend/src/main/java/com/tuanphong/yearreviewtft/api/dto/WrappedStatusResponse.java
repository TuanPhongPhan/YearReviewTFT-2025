package com.tuanphong.yearreviewtft.api.dto;

public record WrappedStatusResponse(
        String puuid,
        int year,
        String state,
        int matchIdsFound,
        int matchesCached,
        boolean summaryReady,
        String message
) {
}

package com.tuanphong.yearreviewtft.api.dto;

public record WrappedRequestResponse(
        String puuid,
        int year,
        String jobId,
        String state
) {}

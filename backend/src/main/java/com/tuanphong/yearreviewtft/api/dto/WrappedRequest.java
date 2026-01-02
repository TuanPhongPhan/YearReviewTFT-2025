package com.tuanphong.yearreviewtft.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record WrappedRequest(
        @NotBlank String riotId,
        @NotBlank String platform, // "EUW1" or "EUN1" (we keep it for later rank endpoints)
        @Min(2020) @Max(2030) int year
) {}

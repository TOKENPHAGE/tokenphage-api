package com.tokenphage.api.feature.sync.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public record TokenRecordRequest(

    @NotBlank(message = "date must not be blank")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "date must be in ISO format (yyyy-MM-dd)")
    String date,

    @NotBlank(message = "model must not be blank")
    String model,

    @PositiveOrZero(message = "inputTok must be zero or positive")
    long inputTok,

    @PositiveOrZero(message = "outputTok must be zero or positive")
    long outputTok,

    @PositiveOrZero(message = "cacheReadTok must be zero or positive")
    long cacheReadTok,

    @PositiveOrZero(message = "cacheCreateTok must be zero or positive")
    long cacheCreateTok
) {}

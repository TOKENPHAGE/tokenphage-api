package com.tokenphage.api.feature.sync.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record SyncRequest(

    @NotBlank(message = "deviceId must not be blank")
    @Pattern(
        regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
        message = "deviceId must be a valid UUID"
    )
    String deviceId,

    @NotEmpty(message = "records must not be empty")
    @Valid
    List<TokenRecordRequest> records
) {}

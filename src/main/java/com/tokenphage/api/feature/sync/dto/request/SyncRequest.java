package com.tokenphage.api.feature.sync.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SyncRequest(

    @NotBlank(message = "deviceId must not be blank")
    @Pattern(
        regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
        message = "deviceId must be a valid UUID"
    )
    String deviceId,

    @NotEmpty(message = "records must not be empty")
    @Size(max = MAX_RECORDS, message = "records must not exceed 10000 entries")
    @Valid
    List<TokenRecordRequest> records
) {

    /**
     * 한 요청의 레코드 상한. 1년치 x 모델 10종 약 3,650 의 여유분이다.
     * JSON 바디는 톰캣 maxPostSize 대상이 아니라 이 제약이 유일한 방어선이다.
     */
    public static final int MAX_RECORDS = 10_000;
}

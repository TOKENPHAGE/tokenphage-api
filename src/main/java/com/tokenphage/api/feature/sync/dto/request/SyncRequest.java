package com.tokenphage.api.feature.sync.dto.request;

import com.tokenphage.api.feature.sync.validation.RecordsOrRuntimesPresent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 동기화 요청. runtimes 는 2026.10.1 부터 보내는 선택 칸이다.
 * <p>
 * runtimes 칸이 없으면(옛 CLI, 보낼 실행시간이 없는 최신 CLI) records 처럼 빈 목록으로 맞춘다.
 * runtimes 항목은 서비스가 하나씩 검사하므로 @Valid 를 붙이지 않는다. 잘못된 항목 하나로 요청 전체가 400 이 되면 안 된다.
 */
@RecordsOrRuntimesPresent
public record SyncRequest(

    @NotBlank(message = "deviceId must not be blank")
    @Pattern(
        regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
        message = "deviceId must be a valid UUID"
    )
    String deviceId,

    @Size(max = MAX_RECORDS, message = "records must not exceed 10000 entries")
    @Valid
    List<TokenRecordRequest> records,

    @Size(max = MAX_RECORDS, message = "runtimes must not exceed 10000 entries")
    List<RuntimeRecordRequest> runtimes
) {

    /**
     * 한 요청의 레코드 상한. 1년치 x 모델 10종 약 3,650 의 여유분이다.
     * JSON 바디는 톰캣 maxPostSize 대상이 아니라 이 제약이 유일한 방어선이다.
     */
    public static final int MAX_RECORDS = 10_000;

    public SyncRequest {
        // 칸이 빠진 요청도 빈 목록과 같은 뜻으로 맞춘다
        if (records == null) {
            records = List.of();
        }
        if (runtimes == null) {
            runtimes = List.of();
        }
    }

    /**
     * runtimes 칸이 없는 요청(옛 CLI 모양)을 만든다.
     *
     * @param deviceId 기기 식별자
     * @param records  토큰 레코드 목록 (null 이면 빈 목록)
     * @Since 2026-10-11
     */
    public SyncRequest(String deviceId, List<TokenRecordRequest> records) {
        this(deviceId, records, null);
    }
}

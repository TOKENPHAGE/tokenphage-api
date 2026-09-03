package com.tokenphage.api.feature.sync.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record TokenRecordRequest(

    @NotBlank(message = "date must not be blank")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "date must be in ISO format (yyyy-MM-dd)")
    String date,

    @NotBlank(message = "model must not be blank")
    @Size(max = MODEL_MAX_LENGTH, message = "model must not exceed 80 characters")
    String model,

    @PositiveOrZero(message = "inputTok must be zero or positive")
    @Max(value = TOKEN_MAX, message = "inputTok must not exceed 1e11")
    long inputTok,

    @PositiveOrZero(message = "outputTok must be zero or positive")
    @Max(value = TOKEN_MAX, message = "outputTok must not exceed 1e11")
    long outputTok,

    @PositiveOrZero(message = "cacheReadTok must be zero or positive")
    @Max(value = TOKEN_MAX, message = "cacheReadTok must not exceed 1e11")
    long cacheReadTok,

    @PositiveOrZero(message = "cacheCreateTok must be zero or positive")
    @Max(value = TOKEN_MAX, message = "cacheCreateTok must not exceed 1e11")
    long cacheCreateTok
) {

    /**
     * daily_token_usage.model 컬럼 폭(V1__init.sql). 초과하면 네이티브 INSERT 에서
     * Postgres 22001 이 나고 @Transactional 이라 sync 배치 전체가 롤백된다.
     */
    public static final int MODEL_MAX_LENGTH = 80;

    /**
     * 토큰 1건 상한. 배지 최고 레벨 임계(1B)의 100배다.
     * <p>
     * 상한을 두는 이유는 집계 SUM 의 bigint 오버플로(Postgres 22003) 차단이다.
     * 한 요청이 레코드 상한을 꽉 채우고 모든 필드가 이 값이어도 4e15 로
     * bigint 범위(9.2e18)에 2000배 이상 여백이 남는다.
     * 여백 검증은 SyncSchemaConstraintTest 가 담당한다.
     */
    public static final long TOKEN_MAX = 100_000_000_000L;
}

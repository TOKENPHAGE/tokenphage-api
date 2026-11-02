package com.tokenphage.api.feature.sync.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 실행시간 한 줄. (날짜, 도구, 모델) 조합 하나의 실행시간 합이다.
 * <p>
 * 칸이 빠진 항목만 걸러내도록 모두 래퍼 타입이다. primitive 면 칸이 빠졌을 때 요청 전체가 400 이 된다.
 * 제약은 컨트롤러가 아니라 RuntimeRecordService 가 항목마다 적용한다. 실제 날짜인지는 서비스가 따로 본다.
 */
public record RuntimeRecordRequest(

    @NotBlank(message = "date must not be blank")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "date must be in ISO format (yyyy-MM-dd)")
    String date,

    @NotNull(message = "source must not be null")
    @Pattern(regexp = SOURCE_REGEX, message = "source must be one of claude, codex, opencode")
    String source,

    @NotBlank(message = "model must not be blank")
    @Size(max = MODEL_MAX_LENGTH, message = "model must not exceed 80 characters")
    String model,

    @NotNull(message = "runtimeMs must not be null")
    @PositiveOrZero(message = "runtimeMs must be zero or positive")
    @Max(value = RUNTIME_MS_MAX, message = "runtimeMs must not exceed 8640000000")
    Long runtimeMs
) {

    /** daily_agent_runtime.model 컬럼 폭(V8). */
    public static final int MODEL_MAX_LENGTH = 80;

    /** 허용하는 도구. V8 의 chk_dar_source 목록과 같아야 한다. */
    public static final String SOURCE_REGEX = "^(claude|codex|opencode)$";

    /** 한 줄의 실행시간 상한. 100일치 ms 다. */
    public static final long RUNTIME_MS_MAX = 8_640_000_000L;
}

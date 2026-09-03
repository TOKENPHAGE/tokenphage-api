package com.tokenphage.api.feature.sync.dto.request;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * sync 입력 DTO 의 제약값이 daily_token_usage 테이블 정의와 수치로 일치하는지 대조한다.
 * <p>
 * 컴파일러가 못 잡는 계약이다 — Bean Validation 애너테이션과 DDL 은 서로를 모른다.
 * 한쪽만 단언하면 반대쪽이 바뀔 때 조용히 통과하므로 양쪽 숫자를 함께 읽어 비교한다.
 * 마이그레이션 SQL 은 classpath 리소스라 외부 파일 의존이 없다(hermetic).
 */
@DisplayName("sync DTO 제약 <-> DB 스키마 정합성")
class SyncSchemaConstraintTest {

    private static final String V1 = "/db/migration/V1__init.sql";
    private static final String V2 = "/db/migration/V2__add_cache_columns.sql";

    private static final List<String> TOKEN_COLUMNS =
            List.of("input_tok", "output_tok", "cache_read_tok", "cache_create_tok");

    /** 단일 디바이스·단일 모델이 윤년 내내 상한을 채웠을 때의 행 수. */
    private static final long WORST_CASE_DAYS = 366L;
    /** 집계식이 한 행에서 더하는 토큰 필드 수. */
    private static final long TOKEN_FIELDS = 4L;
    /** bigint 범위에 남겨둘 안전 여백 배수. */
    private static final long SAFETY_FACTOR = 1_000L;

    private static String sql(String resourcePath) {
        try (InputStream in = SyncSchemaConstraintTest.class.getResourceAsStream(resourcePath)) {
            assertThat(in).as("마이그레이션 리소스 %s", resourcePath).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** DDL 에서 {@code <컬럼> VARCHAR(n)} 의 n 을 뽑는다. 없으면 -1. */
    private static int varcharWidth(String ddl, String column) {
        Matcher m = Pattern.compile(column + "\\s+VARCHAR\\((\\d+)\\)", Pattern.CASE_INSENSITIVE).matcher(ddl);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    private static boolean isBigint(String ddl, String column) {
        return Pattern.compile(column + "\\s+BIGINT\\b", Pattern.CASE_INSENSITIVE).matcher(ddl).find();
    }

    @Nested
    @DisplayName("문자열 컬럼 폭")
    class StringWidth {

        @Test
        @DisplayName("제약대조_model길이_DDL컬럼폭과일치")
        void 제약대조_model길이_DDL컬럼폭과일치() {
            // given: daily_token_usage.model 의 실제 컬럼 폭
            int ddlWidth = varcharWidth(sql(V1), "model");

            // when
            int annotated = TokenRecordRequest.MODEL_MAX_LENGTH;

            // then: 애너테이션이 더 크면 DB 에서 22001 로 터지고, 더 작으면 멀쩡한 값을 400 으로 거절한다
            assertThat(ddlWidth).as("V1__init.sql 의 model VARCHAR(n)").isPositive();
            assertThat(annotated).isEqualTo(ddlWidth);
        }
    }

    @Nested
    @DisplayName("수치 컬럼 한계")
    class NumericLimit {

        @Test
        @DisplayName("제약대조_토큰컬럼4종_전부BIGINT")
        void 제약대조_토큰컬럼4종_전부BIGINT() {
            // given
            String ddl = sql(V1) + "\n" + sql(V2);

            // when / then: INTEGER 로 좁혀지면 TOKEN_MAX 가 컬럼에 들어가지 않는다
            assertThat(TOKEN_COLUMNS)
                    .allSatisfy(column -> assertThat(isBigint(ddl, column))
                            .as("%s 는 BIGINT 여야 한다", column)
                            .isTrue());
        }

        @Test
        @DisplayName("제약대조_토큰상한_1년최악누적도bigint여백내")
        void 제약대조_토큰상한_1년최악누적도bigint여백내() {
            // given: 한 행의 토큰 4필드가 모두 상한이고 그런 행이 366일 쌓인 경우
            long worstYearlySum = TokenRecordRequest.TOKEN_MAX * TOKEN_FIELDS * WORST_CASE_DAYS;

            // when
            long budget = Long.MAX_VALUE / SAFETY_FACTOR;

            // then: 집계 SUM 이 Postgres 22003 을 내지 않도록 1000배 여백을 유지한다
            assertThat(worstYearlySum).isLessThan(budget);
        }

        @Test
        @DisplayName("제약대조_레코드상한_단일요청최악합계도bigint여백내")
        void 제약대조_레코드상한_단일요청최악합계도bigint여백내() {
            // given: 한 요청이 상한만큼의 레코드를 담고 각 레코드가 토큰 상한인 경우
            long worstRequestSum = (long) SyncRequest.MAX_RECORDS * TokenRecordRequest.TOKEN_MAX * TOKEN_FIELDS;

            // when
            long budget = Long.MAX_VALUE / SAFETY_FACTOR;

            // then
            assertThat(worstRequestSum).isLessThan(budget);
        }
    }
}

package com.tokenphage.api.integration;

import com.tokenphage.api.feature.sync.dto.request.RuntimeRecordRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실행시간 입력 DTO 의 제약값이 실제 DB 의 daily_agent_runtime 정의와 수치로 일치하는지 대조한다.
 * <p>
 * Flyway 가 만든 표를 시스템 카탈로그로 조회한다. 어긋나면 DB 에서 22001·23514 로 터지거나 멀쩡한 값을 거부하게 된다.
 */
@Tag("integration")
@SpringBootTest
@DisplayName("실행시간 DTO 제약 <-> DB 스키마 정합성")
class RuntimeRecordSchemaConstraintIntegrationTest extends ContainerSupport {

    private static final String TABLE = "daily_agent_runtime";

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("model 최대 길이가 DB model 컬럼 폭과 같다")
    void 제약대조_model길이_DB컬럼폭과일치() {
        // given: daily_agent_runtime.model 의 실제 컬럼 폭
        Integer dbWidth = column("character_maximum_length", "model", Integer.class);

        // when
        int annotated = RuntimeRecordRequest.MODEL_MAX_LENGTH;

        // then: 애너테이션이 더 크면 DB 에서 22001 로 터지고, 더 작으면 멀쩡한 값을 거부한다
        assertThat(annotated).isEqualTo(dbWidth);
    }

    @Test
    @DisplayName("source 정규식의 허용값이 DB CHECK 목록과 같다")
    void 제약대조_source정규식_DB_CHECK목록과일치() {
        // given
        Set<String> checked = checkedValues("chk_dar_source");

        // when
        Set<String> allowed = regexSources(RuntimeRecordRequest.SOURCE_REGEX);

        // then: 서버에서 통과한 값이 CHECK(23514)에 걸려 요청 전체가 500 이 되면 안 된다
        assertThat(allowed).isEqualTo(checked);
    }

    @Test
    @DisplayName("runtimeMs 상한은 INTEGER 를 넘고 DB runtime_ms 는 bigint 다")
    void 제약대조_runtimeMs상한_DB_BIGINT컬럼에들어감() {
        // given
        String dbType = column("data_type", "runtime_ms", String.class);

        // when
        long max = RuntimeRecordRequest.RUNTIME_MS_MAX;

        // then: 상한(100일치 ms)이 INTEGER 최댓값보다 커서 컬럼이 INTEGER 로 좁혀지면 22003 이 난다
        assertThat(max).isGreaterThan(Integer.MAX_VALUE);
        assertThat(dbType).isEqualTo("bigint");
    }

    /** information_schema.columns 에서 이 표의 컬럼 속성 하나를 읽는다. */
    private <T> T column(String attribute, String column, Class<T> type) {
        return jdbc.queryForObject("SELECT " + attribute + " FROM information_schema.columns"
                + " WHERE table_schema = current_schema() AND table_name = ? AND column_name = ?", type, TABLE, column);
    }

    /** CHECK 제약 정의에서 따옴표로 묶인 허용값을 모은다. */
    private Set<String> checkedValues(String constraint) {
        String definition = jdbc.queryForObject(
                "SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = ?", String.class, constraint);
        Set<String> values = new HashSet<>();
        Matcher m = Pattern.compile("'([^']+)'").matcher(definition);
        while (m.find()) {
            values.add(m.group(1));
        }
        return values;
    }

    /** {@code ^(a|b)$} 형태 정규식의 선택지 목록. */
    private static Set<String> regexSources(String regex) {
        Matcher m = Pattern.compile("^\\^\\((.*)\\)\\$$").matcher(regex);
        assertThat(m.matches()).as("source 정규식 %s", regex).isTrue();
        return Set.of(m.group(1).split("\\|"));
    }
}

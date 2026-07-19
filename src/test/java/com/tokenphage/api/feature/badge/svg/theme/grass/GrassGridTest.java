package com.tokenphage.api.feature.badge.svg.theme.grass;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GrassGrid 단위 테스트")
class GrassGridTest {

    // 2026-07-15는 수요일 (row=3)
    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 7, 15);

    @Nested
    @DisplayName("열/행 매핑 — 캘린더 정렬")
    class ColumnRowMapping {

        @Test
        @DisplayName("그리드_오늘_마지막열매핑")
        void 그리드_오늘_마지막열매핑() {
            // given
            LocalDate today = WEDNESDAY;
            // when
            int col = GrassGrid.columnOf(today, today);
            int row = GrassGrid.rowOf(today);
            // then
            assertThat(col).isEqualTo(51);
            assertThat(row).isEqualTo(3);
        }

        @Test
        @DisplayName("그리드_일요일시작정렬_52주커버")
        void 그리드_일요일시작정렬_52주커버() {
            // given
            LocalDate today = WEDNESDAY;
            // when
            LocalDate start = GrassGrid.gridStart(today);
            // then: 시작일은 일요일(row 0), col 0이며, 창 전체가 51*7 + (오늘 요일 index) + 1일
            assertThat(start.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
            assertThat(GrassGrid.columnOf(start, today)).isZero();
            assertThat(GrassGrid.rowOf(start)).isZero();
            long windowDays = ChronoUnit.DAYS.between(start, today) + 1;
            assertThat(windowDays).isEqualTo(51 * 7 + GrassGrid.rowOf(today) + 1);
        }

        @Test
        @DisplayName("그리드_요일별_행매핑")
        void 그리드_요일별_행매핑() {
            // given: 2026-07-12(일) ~ 2026-07-18(토)
            LocalDate sunday = LocalDate.of(2026, 7, 12);
            // when // then: Sun=0 .. Sat=6 (GitHub 정렬)
            for (int i = 0; i < 7; i++) {
                assertThat(GrassGrid.rowOf(sunday.plusDays(i))).isEqualTo(i);
            }
        }
    }

    @Nested
    @DisplayName("월 라벨")
    class MonthLabels {

        @Test
        @DisplayName("그리드_월경계열_라벨위치")
        void 그리드_월경계열_라벨위치() {
            // given
            LocalDate today = WEDNESDAY;
            LocalDate start = GrassGrid.gridStart(today);
            // when
            List<GrassGrid.MonthLabel> labels = GrassGrid.monthLabels(today);
            // then: 첫 라벨은 col0에 gridStart의 달, 이후 라벨의 열은 해당 열 일요일이 새 달인 첫 열
            assertThat(labels.get(0).column()).isZero();
            assertThat(labels.get(0).label())
                    .isEqualTo(GrassGrid.monthShortName(start.getMonthValue()));
            for (GrassGrid.MonthLabel label : labels.subList(1, labels.size())) {
                LocalDate colSunday = start.plusWeeks(label.column());
                LocalDate prevColSunday = colSunday.minusWeeks(1);
                assertThat(colSunday.getMonth()).isNotEqualTo(prevColSunday.getMonth());
                assertThat(label.label())
                        .isEqualTo(GrassGrid.monthShortName(colSunday.getMonthValue()));
            }
        }

        @Test
        @DisplayName("그리드_첫열부분월_라벨표기")
        void 그리드_첫열부분월_라벨표기() {
            // given: gridStart가 달 중간(rolling 창)이어도 col0에 그 달 라벨을 찍는다 (GitHub 동일)
            LocalDate today = WEDNESDAY;
            LocalDate start = GrassGrid.gridStart(today);
            // when
            List<GrassGrid.MonthLabel> labels = GrassGrid.monthLabels(today);
            // then
            assertThat(start.getDayOfMonth()).isNotEqualTo(1); // 전제: 실제로 부분월
            assertThat(labels.get(0).column()).isZero();
            assertThat(labels.get(0).label()).isEqualTo("Jul");
        }

        @Test
        @DisplayName("그리드_인접월라벨_2열미만생략")
        void 그리드_인접월라벨_2열미만생략() {
            // given: gridStart=2025-01-26(일) → col1 일요일=2025-02-02, 월 경계가 col0과 1열 차이
            //        today = gridStart + 51주가 속한 주의 화요일
            LocalDate today = LocalDate.of(2025, 1, 26).plusWeeks(51).plusDays(2);
            assertThat(GrassGrid.gridStart(today)).isEqualTo(LocalDate.of(2025, 1, 26));
            // when
            List<GrassGrid.MonthLabel> labels = GrassGrid.monthLabels(today);
            // then: col0=Jan 유지, col1의 Feb 라벨은 직전 라벨과 2열 미만이라 생략
            assertThat(labels.get(0).column()).isZero();
            assertThat(labels.get(0).label()).isEqualTo("Jan");
            assertThat(labels).noneMatch(l -> l.column() == 1);
            assertThat(labels.get(1).label()).isEqualTo("Mar");
        }

        @Test
        @DisplayName("그리드_라벨간격_전부2열이상")
        void 그리드_라벨간격_전부2열이상() {
            // given
            LocalDate today = WEDNESDAY;
            // when
            List<GrassGrid.MonthLabel> labels = GrassGrid.monthLabels(today);
            // then: 12~13개 라벨, 인접 라벨 간격은 항상 2열 이상
            assertThat(labels).hasSizeBetween(11, 13);
            for (int i = 1; i < labels.size(); i++) {
                assertThat(labels.get(i).column() - labels.get(i - 1).column())
                        .isGreaterThanOrEqualTo(2);
            }
        }
    }

    @Nested
    @DisplayName("잔디 강도 레벨")
    class Levels {

        @Test
        @DisplayName("레벨_경계비율_5단계")
        void 레벨_경계비율_5단계() {
            // given
            long max = 100;
            // when // then: 0=empty, <0.2/<0.4/<0.6/<0.8/이상 (CardBadgeTheme.heatColor와 동일 경계)
            assertThat(GrassGrid.levelFor(0, max)).isZero();
            assertThat(GrassGrid.levelFor(19, max)).isEqualTo(1);
            assertThat(GrassGrid.levelFor(20, max)).isEqualTo(2);
            assertThat(GrassGrid.levelFor(39, max)).isEqualTo(2);
            assertThat(GrassGrid.levelFor(40, max)).isEqualTo(3);
            assertThat(GrassGrid.levelFor(59, max)).isEqualTo(3);
            assertThat(GrassGrid.levelFor(60, max)).isEqualTo(4);
            assertThat(GrassGrid.levelFor(79, max)).isEqualTo(4);
            assertThat(GrassGrid.levelFor(80, max)).isEqualTo(5);
            assertThat(GrassGrid.levelFor(100, max)).isEqualTo(5);
        }

        @Test
        @DisplayName("레벨_max0_전부empty")
        void 레벨_max0_전부empty() {
            // given // when // then: 데이터 전무 유저 — 0-division 없이 전부 empty
            assertThat(GrassGrid.levelFor(0, 0)).isZero();
            assertThat(GrassGrid.levelFor(5, 0)).isZero();
        }
    }
}

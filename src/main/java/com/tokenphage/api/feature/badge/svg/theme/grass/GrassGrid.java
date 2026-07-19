package com.tokenphage.api.feature.badge.svg.theme.grass;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 잔디 그리드(52주 x 7일)의 순수 날짜 계산 유틸.
 * <p>
 * GitHub contribution graph 정렬을 따른다: 열=주(일요일 시작), 행=요일(Sun=0..Sat=6),
 * 마지막 열=오늘이 속한 주. SVG 좌표 조립은 GrassBadgeTheme이 담당하고
 * 이 클래스는 열/행/월 라벨/강도 레벨 계산만 책임진다.
 */
final class GrassGrid {

    /** 그리드 열 수 (52주). */
    static final int COLUMNS = 52;

    /** 월 라벨 최소 간격(열) — 이보다 가까우면 뒤 라벨을 생략한다. */
    private static final int MIN_LABEL_GAP = 2;

    private static final String[] MONTH_SHORT_NAMES = {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    /** 월 라벨 배치 결과 — column은 0..51, label은 영문 3글자 월명. */
    record MonthLabel(int column, String label) {
    }

    private GrassGrid() {
    }

    /**
     * 그리드 시작일(왼쪽 끝 열의 일요일)을 반환한다.
     *
     * @param today 조회 기준일 (null 불허)
     * @return 오늘이 속한 주의 일요일에서 51주 전 일요일
     * @Since 2026-07-15
     */
    static LocalDate gridStart(LocalDate today) {
        return sundayOf(today).minusWeeks(COLUMNS - 1L);
    }

    /**
     * 날짜가 속한 열 인덱스를 반환한다. 최신 주가 항상 마지막 열(51)이 된다.
     *
     * @param date  대상 날짜 (그리드 창 내, null 불허)
     * @param today 조회 기준일 (null 불허)
     * @return 0(가장 오래된 주)..51(이번 주)
     * @Since 2026-07-15
     */
    static int columnOf(LocalDate date, LocalDate today) {
        return (int) (ChronoUnit.DAYS.between(gridStart(today), date) / 7);
    }

    /**
     * 날짜의 행 인덱스를 반환한다 (Sun=0 .. Sat=6, GitHub 정렬).
     *
     * @param date 대상 날짜 (null 불허)
     * @return 0..6
     * @Since 2026-07-15
     */
    static int rowOf(LocalDate date) {
        return date.getDayOfWeek().getValue() % 7;
    }

    /**
     * 월 라벨 배치를 계산한다.
     * <p>
     * col0은 부분월이어도 항상 라벨을 찍고(GitHub 동일), 이후에는 열의 일요일 날짜가
     * 새 달로 바뀌는 첫 열에 라벨을 둔다. 직전 라벨과의 간격이 2열 미만이면 생략한다.
     *
     * @param today 조회 기준일 (null 불허)
     * @return 열 오름차순 월 라벨 목록
     * @Since 2026-07-15
     */
    static List<MonthLabel> monthLabels(LocalDate today) {
        LocalDate start = gridStart(today);
        List<MonthLabel> labels = new ArrayList<>();
        int lastLabeledColumn = 0;
        labels.add(new MonthLabel(0, monthShortName(start.getMonthValue())));
        for (int col = 1; col < COLUMNS; col++) {
            LocalDate colSunday = start.plusWeeks(col);
            if (colSunday.getMonth() == colSunday.minusWeeks(1).getMonth()) {
                continue;
            }
            if (col - lastLabeledColumn < MIN_LABEL_GAP) {
                continue;
            }
            labels.add(new MonthLabel(col, monthShortName(colSunday.getMonthValue())));
            lastLabeledColumn = col;
        }
        return List.copyOf(labels);
    }

    /**
     * 사용량의 잔디 강도 레벨을 반환한다.
     * <p>
     * 그리드 창 내 최대값 대비 비율로 5단계를 나눈다 (CardBadgeTheme.heatColor와 동일 경계).
     *
     * @param val 해당 일의 토큰 수
     * @param max 그리드 창 내 최대 토큰 수
     * @return 0(empty) 또는 1..5
     * @Since 2026-07-15
     */
    static int levelFor(long val, long max) {
        if (max == 0 || val == 0) {
            return 0;
        }
        double ratio = (double) val / max;
        if (ratio < 0.2) {
            return 1;
        }
        if (ratio < 0.4) {
            return 2;
        }
        if (ratio < 0.6) {
            return 3;
        }
        if (ratio < 0.8) {
            return 4;
        }
        return 5;
    }

    /**
     * 월 번호(1..12)의 영문 3글자 월명을 반환한다.
     *
     * @param monthValue 1..12
     * @return "Jan".."Dec"
     * @Since 2026-07-15
     */
    static String monthShortName(int monthValue) {
        return MONTH_SHORT_NAMES[monthValue - 1];
    }

    private static LocalDate sundayOf(LocalDate date) {
        return date.minusDays(rowOf(date));
    }
}

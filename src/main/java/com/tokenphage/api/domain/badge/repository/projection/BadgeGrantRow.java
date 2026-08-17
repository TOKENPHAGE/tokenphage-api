package com.tokenphage.api.domain.badge.repository.projection;

/**
 * 배지 사용 가능 여부와 잠금 안내 문구를 담는 조회 결과.
 * <p>
 * 배지별 안내 문구가 필요해 두 값을 함께 조회한다.
 * SQL 별칭과 getter 이름을 camelCase로 맞춰야 매핑된다.
 */
public interface BadgeGrantRow {

    /**
     * 쓸 수 있으면 true.
     * <p>
     * primitive다 — {@code require_grant}가 NOT NULL이고 EXISTS도 NULL을 반환하지 않는다.
     * 등록되지 않은 코드는 조회 결과가 없어 이 객체 자체가 null로 온다.
     */
    boolean getGranted();

    /** 배지 표시명. 잠금 안내의 제목으로 쓰인다. */
    String getDisplayName();

    /** 자격이 없을 때 보여줄 안내 문구. NULL이면 호출 측이 기본 문구를 쓴다. */
    String getLockedMessage();
}

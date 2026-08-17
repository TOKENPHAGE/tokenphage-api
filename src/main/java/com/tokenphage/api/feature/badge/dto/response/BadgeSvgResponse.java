package com.tokenphage.api.feature.badge.dto.response;

/**
 * 배지 SVG와 사용 가능 여부를 함께 전달하는 응답 DTO.
 * <p>
 * 거부도 200으로 나가 상태코드로 구분되지 않는다. 컨트롤러가 접근 기록에 남길 때 쓴다.
 *
 * @param svg     완성된 SVG 문자열
 * @param granted 정상 배지면 true, 잠금 안내면 false
 */
public record BadgeSvgResponse(String svg, boolean granted) {
}

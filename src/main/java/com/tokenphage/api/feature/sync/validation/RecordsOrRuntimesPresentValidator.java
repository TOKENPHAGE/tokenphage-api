package com.tokenphage.api.feature.sync.validation;

import com.tokenphage.api.feature.sync.dto.request.SyncRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** {@link RecordsOrRuntimesPresent} 검사기. */
public class RecordsOrRuntimesPresentValidator implements ConstraintValidator<RecordsOrRuntimesPresent, SyncRequest> {

    /**
     * records 나 runtimes 중 하나라도 항목이 있으면 통과시킨다.
     * <p>
     * 위반은 타입 경로 대신 records 경로에 건다. 그래야 400 본문이 기존 records 위반과 같다.
     * 두 칸은 SyncRequest 가 빈 목록으로 맞춰서 null 이 오지 않는다.
     *
     * @param req     검사할 동기화 요청 (null 이면 통과)
     * @param context 위반 경로를 바꾸는 데 쓰는 검증 컨텍스트
     * @return 둘 중 하나라도 비어 있지 않으면 true
     * @Since 2026-10-29
     */
    @Override
    public boolean isValid(SyncRequest req, ConstraintValidatorContext context) {
        if (req == null) {
            return true;
        }
        boolean hasRecords = !req.records().isEmpty();
        boolean hasRuntimes = !req.runtimes().isEmpty();
        if (hasRecords || hasRuntimes) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("records")
                .addConstraintViolation();
        return false;
    }
}

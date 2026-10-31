package com.tokenphage.api.feature.sync.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * records 와 runtimes 중 하나는 비어 있지 않아야 한다는 타입 수준 제약.
 * <p>
 * 위반은 records 필드에 기존 문구로 건다. 옛 CLI 가 빈 records 를 보냈을 때의 400 본문을 그대로 두기 위해서다.
 */
@Documented
@Constraint(validatedBy = RecordsOrRuntimesPresentValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RecordsOrRuntimesPresent {

    String message() default "records must not be empty";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

package com.atguigu.daijia.model.validate.map.anno;

import com.atguigu.daijia.model.validate.map.LongitudeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = LongitudeValidator.class)
public @interface ValidLongitude {
    String message() default "经度必须在 -180 到 180 之间";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}


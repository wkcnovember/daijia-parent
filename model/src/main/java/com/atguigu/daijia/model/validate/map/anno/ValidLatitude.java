package com.atguigu.daijia.model.validate.map.anno;

import com.atguigu.daijia.model.validate.map.LatitudeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = LatitudeValidator.class)
public @interface ValidLatitude {
    String message() default "纬度必须在 -90 到 90 之间";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

package com.atguigu.daijia.model.validate.map;

import com.atguigu.daijia.model.validate.map.anno.ValidLongitude;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public class LongitudeValidator implements ConstraintValidator<ValidLongitude, BigDecimal> {
    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        return value.compareTo(new BigDecimal("-180")) >= 0 && value.compareTo(new BigDecimal("180")) <= 0;
    }
}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EnumValueValidator implements ConstraintValidator<ValidEnum, String> {
  private ValidEnum annotation;

  @Override
  public void initialize(ValidEnum annotation) {
    this.annotation = annotation;
  }

  @Override
  public boolean isValid(String valueForValidation,
      ConstraintValidatorContext constraintValidatorContext) {
    if (valueForValidation == null && this.annotation.allowNulls()) {
      return true;
    }

    Object[] enumValues = this.annotation.enumClass().getEnumConstants();
    if (enumValues != null && valueForValidation != null) {
      for (Object enumValue : enumValues) {
        if (valueForValidation.equals(enumValue.toString())
            || (this.annotation.ignoreCase()
                && valueForValidation.equalsIgnoreCase(enumValue.toString()))) {
          return true;
        }
      }
    }

    return false;
  }
}

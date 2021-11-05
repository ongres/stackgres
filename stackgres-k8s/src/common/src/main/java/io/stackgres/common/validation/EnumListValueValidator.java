/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.validation;

import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class EnumListValueValidator implements ConstraintValidator<ValidEnumList, List<String>> {
  private ValidEnumList annotation;

  @Override
  public void initialize(ValidEnumList annotation) {
    this.annotation = annotation;
  }

  @Override
  public boolean isValid(List<String> valueForValidation,
      ConstraintValidatorContext constraintValidatorContext) {
    if (valueForValidation == null) {
      return true;
    }

    for (String valueElementForValidation : valueForValidation) {
      if (!isValid(valueElementForValidation)) {
        return false;
      }
    }

    return true;
  }

  private boolean isValid(String valueElementForValidation) {
    if (valueElementForValidation == null && this.annotation.allowNulls()) {
      return true;
    }

    Object[] enumValues = this.annotation.enumClass().getEnumConstants();
    if (enumValues != null && valueElementForValidation != null) {
      for (Object enumValue : enumValues) {
        if (valueElementForValidation.equals(enumValue.toString())
            || (this.annotation.ignoreCase()
                && valueElementForValidation.equalsIgnoreCase(enumValue.toString()))) {
          return true;
        }
      }
    }

    return false;
  }
}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.ErrorType;
import jakarta.inject.Qualifier;

@Qualifier
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RegisterForReflection
public @interface ValidationType {

  /**
   * The error type that the a validation could throw.
   * @return The error type
   */
  ErrorType value();
}

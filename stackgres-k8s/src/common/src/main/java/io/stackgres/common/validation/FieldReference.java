/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Payload;

public interface FieldReference extends Payload {

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @interface ReferencedField {
    String value();
  }

}

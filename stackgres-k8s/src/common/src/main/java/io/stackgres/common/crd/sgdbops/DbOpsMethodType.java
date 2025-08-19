/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import org.jetbrains.annotations.NotNull;

public enum DbOpsMethodType {

  IN_PLACE("InPlace", "in-place"),
  REDUCED_IMPACT("ReducedImpact", "reduced-impact");

  private final @NotNull String type;
  private final @NotNull String annotationValue;

  DbOpsMethodType(@NotNull String type, String annotationValue) {
    this.type = type;
    this.annotationValue = annotationValue;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }

  public @NotNull String annotationValue() {
    return annotationValue;
  }

  public static DbOpsMethodType fromString(String from) {
    for (DbOpsMethodType value : values()) {
      if (value.type.equals(from)) {
        return value;
      }
    }
    throw new IllegalArgumentException("method type is invalid: " + from);
  }

  public static DbOpsMethodType fromAnnotationValue(String annotationValue) {
    for (DbOpsMethodType value : values()) {
      if (value.annotationValue.equals(annotationValue)) {
        return value;
      }
    }
    throw new IllegalArgumentException("method is invalid: " + annotationValue);
  }

}

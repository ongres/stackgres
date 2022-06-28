/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.util.Objects;

import javax.enterprise.util.AnnotationLiteral;

import io.stackgres.common.StackGresContainer;

public class SidecarLiteral extends AnnotationLiteral<Sidecar> implements Sidecar {

  private static final long serialVersionUID = 1L;

  private final StackGresContainer value;

  public SidecarLiteral(StackGresContainer value) {
    this.value = value;
  }

  @Override
  public StackGresContainer value() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    SidecarLiteral that = (SidecarLiteral) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), value);
  }
}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.Objects;

import io.fabric8.kubernetes.client.CustomResource;
import jakarta.enterprise.util.AnnotationLiteral;

public class ReconciliationScopeLiteral extends AnnotationLiteral<ReconciliationScope>
    implements ReconciliationScope {

  private static final long serialVersionUID = 1L;

  Class<? extends CustomResource<?, ?>> value;

  String kind;

  public ReconciliationScopeLiteral(Class<? extends CustomResource<?, ?>> value, String kind) {
    this.value = value;
    this.kind = kind;
  }

  @Override
  public Class<? extends CustomResource<?, ?>> value() {
    return value;
  }

  @Override
  public String kind() {
    return kind;
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
    ReconciliationScopeLiteral that = (ReconciliationScopeLiteral) o;
    return Objects.equals(value, that.value)
        && Objects.equals(kind, that.kind);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), value, kind);
  }
}

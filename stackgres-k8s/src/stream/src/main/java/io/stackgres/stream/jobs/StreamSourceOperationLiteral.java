/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs;

import java.util.Objects;

import io.stackgres.common.crd.sgstream.StreamSourceType;
import jakarta.enterprise.util.AnnotationLiteral;

public class StreamSourceOperationLiteral extends AnnotationLiteral<StreamSourceOperation>
    implements StreamSourceOperation {

  private static final long serialVersionUID = 1L;

  private final StreamSourceType value;

  public StreamSourceOperationLiteral(StreamSourceType value) {
    this.value = value;
  }

  @Override
  public StreamSourceType value() {
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
    StreamSourceOperationLiteral that = (StreamSourceOperationLiteral) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), value);
  }
}

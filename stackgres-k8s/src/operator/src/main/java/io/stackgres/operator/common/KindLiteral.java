/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import javax.enterprise.util.AnnotationLiteral;

import io.fabric8.kubernetes.api.model.HasMetadata;

public class KindLiteral extends AnnotationLiteral<Kind> implements Kind {

  private static final long serialVersionUID = 1L;

  private final Class<? extends HasMetadata> value;

  public KindLiteral(Class<? extends HasMetadata> value) {
    this.value = value;
  }

  @Override
  public Class<? extends HasMetadata> value() {
    return value;
  }

}

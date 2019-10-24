/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import javax.enterprise.util.AnnotationLiteral;

public class KindLiteral extends AnnotationLiteral<Kind> implements Kind {

  private static final long serialVersionUID = 1L;

  private final String value;

  public KindLiteral(String value) {
    this.value = value;
  }

  @Override
  public String value() {
    return value;
  }
}

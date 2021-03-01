/*
 * Copyright (C) 2021 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

/**
 * Build-time constants properties.
 */
public final class CommonDefinition {

  private CommonDefinition() {
    throw new AssertionError();
  }

  public static final String GROUP = "${stackgres.group}";
  public static final String VERSION = "${stackgres.crd.version}";

}

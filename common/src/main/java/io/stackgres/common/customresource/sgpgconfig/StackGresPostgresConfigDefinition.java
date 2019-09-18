/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.customresource.sgpgconfig;

public class StackGresPostgresConfigDefinition {

  public static final String GROUP = "stackgres.io";
  public static final String VERSION = "v1alpha1";
  public static final String KIND = "StackGresPostgresConfig";
  public static final String SINGULAR = "sgpgconfig";
  public static final String PLURAL = "sgpgconfigs";
  public static final String NAME = PLURAL + "." + GROUP;
  public static final String APIVERSION = GROUP + "/" + VERSION;

  private StackGresPostgresConfigDefinition() {
    throw new AssertionError("No instances for you!");
  }

}

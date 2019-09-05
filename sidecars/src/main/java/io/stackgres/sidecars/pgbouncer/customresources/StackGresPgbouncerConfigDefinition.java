/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.sidecars.pgbouncer.customresources;

public class StackGresPgbouncerConfigDefinition {

  public static final String GROUP = "stackgres.io";
  public static final String VERSION = "v1alpha1";
  public static final String KIND = "StackGresPgbouncerConfig";
  public static final String SINGULAR = "sgpgbouncerconfig";
  public static final String PLURAL = "sgpgbouncerconfigs";
  public static final String NAME = PLURAL + "." + GROUP;
  public static final String APIVERSION = GROUP + "/" + VERSION;

  private StackGresPgbouncerConfigDefinition() {
    throw new AssertionError("No instances for you!");
  }

}

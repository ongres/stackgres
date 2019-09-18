/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.customresource.sgprofile;

public class StackGresProfileDefinition {

  public static final String GROUP = "stackgres.io";
  public static final String VERSION = "v1alpha1";
  public static final String KIND = "StackGresProfile";
  public static final String SINGULAR = "sgprofile";
  public static final String PLURAL = "sgprofiles";
  public static final String NAME = PLURAL + "." + GROUP;
  public static final String APIVERSION = GROUP + "/" + VERSION;

  private StackGresProfileDefinition() {
    throw new AssertionError("No instances for you!");
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.sgcluster;

public class StackGresClusterDefinition {

  public static final String GROUP = "stackgres.io";
  public static final String VERSION = "v1alpha1";
  public static final String KIND = "StackGresCluster";
  public static final String SINGULAR = "sgcluster";
  public static final String PLURAL = "sgclusters";
  public static final String NAME = PLURAL + "." + GROUP;
  public static final String APIVERSION = GROUP + "/" + VERSION;

  private StackGresClusterDefinition() {
    throw new AssertionError("No instances for you!");
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import io.stackgres.common.StackGresProperty;

public enum StackGresClusterDefinition {

  ;

  public static final String KIND = "SGCluster";
  public static final String SINGULAR = "sgcluster";
  public static final String PLURAL = "sgclusters";
  public static final String NAME = PLURAL + "." + StackGresProperty.CRD_GROUP.getString();
  public static final String APIVERSION = StackGresProperty.CRD_GROUP.getString()
      + "/" + StackGresProperty.CRD_VERSION.getString();

}

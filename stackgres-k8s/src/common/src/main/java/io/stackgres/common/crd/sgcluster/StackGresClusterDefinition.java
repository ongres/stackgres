/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import io.stackgres.common.StackGresContext;

public enum StackGresClusterDefinition {

  ;

  public static final String KIND = "SGCluster";
  public static final String SINGULAR = "sgcluster";
  public static final String PLURAL = "sgclusters";
  public static final String NAME = PLURAL + "." + StackGresContext.CRD_GROUP;
  public static final String APIVERSION = StackGresContext.CRD_GROUP
      + "/" + StackGresContext.CRD_VERSION;

}

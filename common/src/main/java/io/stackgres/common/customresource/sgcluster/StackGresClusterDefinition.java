/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.customresource.sgcluster;

import io.stackgres.common.StackGresUtil;

public enum StackGresClusterDefinition {

  ;

  public static final String KIND = "StackGresCluster";
  public static final String SINGULAR = "sgcluster";
  public static final String PLURAL = "sgclusters";
  public static final String NAME = PLURAL + "." + StackGresUtil.CRD_GROUP;
  public static final String APIVERSION = StackGresUtil.CRD_GROUP + "/" + StackGresUtil.CRD_VERSION;

}

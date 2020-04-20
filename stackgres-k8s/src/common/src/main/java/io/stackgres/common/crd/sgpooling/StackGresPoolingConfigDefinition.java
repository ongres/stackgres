/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgpooling;

import io.stackgres.common.StackGresContext;

public enum StackGresPoolingConfigDefinition {

  ;

  public static final String KIND = "SGPoolingConfig";
  public static final String SINGULAR = "sgpoolconfig";
  public static final String PLURAL = "sgpoolconfigs";
  public static final String NAME = PLURAL + "." + StackGresContext.CRD_GROUP;
  public static final String APIVERSION = StackGresContext.CRD_GROUP
      + "/" + StackGresContext.CRD_VERSION;

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgpgconfig;

import io.stackgres.common.StackGresContext;

public enum StackGresPostgresConfigDefinition {

  ;

  public static final String KIND = "SGPostgresConfig";
  public static final String SINGULAR = "sgpgconfig";
  public static final String PLURAL = "sgpgconfigs";
  public static final String NAME = PLURAL + "." + StackGresContext.CRD_GROUP;
  public static final String APIVERSION = StackGresContext.CRD_GROUP
      + "/" + StackGresContext.CRD_VERSION;

}

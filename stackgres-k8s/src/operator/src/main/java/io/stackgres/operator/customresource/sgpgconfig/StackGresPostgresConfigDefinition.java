/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgpgconfig;

import io.stackgres.operator.common.StackGresUtil;

public enum StackGresPostgresConfigDefinition {

  ;

  public static final String KIND = "SGPostgresConfig";
  public static final String SINGULAR = "sgpgconfig";
  public static final String PLURAL = "sgpgconfigs";
  public static final String NAME = PLURAL + "." + StackGresUtil.CRD_GROUP;
  public static final String APIVERSION = StackGresUtil.CRD_GROUP + "/" + StackGresUtil.CRD_VERSION;

}

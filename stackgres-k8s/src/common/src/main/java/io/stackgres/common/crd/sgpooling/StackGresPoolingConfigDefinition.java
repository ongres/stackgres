/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgpooling;

import io.stackgres.common.StackGresProperty;

public enum StackGresPoolingConfigDefinition {

  ;

  public static final String KIND = "SGPoolingConfig";
  public static final String SINGULAR = "sgpoolconfig";
  public static final String PLURAL = "sgpoolconfigs";
  public static final String NAME = PLURAL + "." + StackGresProperty.CRD_GROUP.getString();
  public static final String APIVERSION = StackGresProperty.CRD_GROUP.getString()
      + "/" + StackGresProperty.CRD_VERSION.getString();

}

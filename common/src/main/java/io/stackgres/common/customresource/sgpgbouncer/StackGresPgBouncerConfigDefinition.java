/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.customresource.sgpgbouncer;

import io.stackgres.common.StackGresUtil;

public enum StackGresPgBouncerConfigDefinition {

  ;

  public static final String KIND = "StackGresPgBouncerConfig";
  public static final String SINGULAR = "sgpgbouncer";
  public static final String PLURAL = "sgpgbouncers";
  public static final String NAME = PLURAL + "." + StackGresUtil.GROUP;
  public static final String APIVERSION = StackGresUtil.GROUP + "/" + StackGresUtil.CRD_VERSION;

}

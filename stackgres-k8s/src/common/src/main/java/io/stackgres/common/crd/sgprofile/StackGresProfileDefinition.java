/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgprofile;

import io.stackgres.common.StackGresContext;

public enum StackGresProfileDefinition {

  ;

  public static final String KIND = "SGInstanceProfile";
  public static final String SINGULAR = "sginstanceprofile";
  public static final String PLURAL = "sginstanceprofiles";
  public static final String NAME = PLURAL + "." + StackGresContext.CRD_GROUP;
  public static final String APIVERSION = StackGresContext.CRD_GROUP
      + "/" + StackGresContext.CRD_VERSION;

}

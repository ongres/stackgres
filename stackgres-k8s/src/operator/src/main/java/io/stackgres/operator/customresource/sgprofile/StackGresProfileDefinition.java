/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgprofile;

import io.stackgres.operator.common.StackGresUtil;

public enum StackGresProfileDefinition {

  ;

  public static final String KIND = "SGInstanceProfile";
  public static final String SINGULAR = "sginstanceprofile";
  public static final String PLURAL = "sginstanceprofiles";
  public static final String NAME = PLURAL + "." + StackGresUtil.CRD_GROUP;
  public static final String APIVERSION = StackGresUtil.CRD_GROUP + "/" + StackGresUtil.CRD_VERSION;

}

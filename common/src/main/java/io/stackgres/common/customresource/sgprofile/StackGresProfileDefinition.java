/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.customresource.sgprofile;

import io.stackgres.common.StackGresUtil;

public enum StackGresProfileDefinition {

  ;

  public static final String KIND = "StackGresProfile";
  public static final String SINGULAR = "sgprofile";
  public static final String PLURAL = "sgprofiles";
  public static final String NAME = PLURAL + "." + StackGresUtil.CRD_GROUP;
  public static final String APIVERSION = StackGresUtil.CRD_GROUP + "/" + StackGresUtil.CRD_VERSION;

}

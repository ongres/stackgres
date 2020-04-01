/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgbackupconfig;

import io.stackgres.operator.common.StackGresUtil;

public enum StackGresBackupConfigDefinition {

  ;

  public static final String KIND = "SGBackupConfig";
  public static final String SINGULAR = "sgbackupconfig";
  public static final String PLURAL = "sgbackupconfigs";
  public static final String NAME = PLURAL + "." + StackGresUtil.CRD_GROUP;
  public static final String APIVERSION = StackGresUtil.CRD_GROUP + "/" + StackGresUtil.CRD_VERSION;

}

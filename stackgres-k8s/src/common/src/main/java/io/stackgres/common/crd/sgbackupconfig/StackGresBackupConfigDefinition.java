/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgbackupconfig;

import io.stackgres.common.StackGresContext;

public enum StackGresBackupConfigDefinition {

  ;

  public static final String KIND = "SGBackupConfig";
  public static final String SINGULAR = "sgbackupconfig";
  public static final String PLURAL = "sgbackupconfigs";
  public static final String NAME = PLURAL + "." + StackGresContext.CRD_GROUP;
  public static final String APIVERSION = StackGresContext.CRD_GROUP
      + "/" + StackGresContext.CRD_VERSION;

}

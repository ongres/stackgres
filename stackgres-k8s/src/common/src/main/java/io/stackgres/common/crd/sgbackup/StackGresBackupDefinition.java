/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgbackup;

import io.stackgres.common.StackGresContext;

public enum StackGresBackupDefinition {

  ;

  public static final String KIND = "SGBackup";
  public static final String SINGULAR = "sgbackup";
  public static final String PLURAL = "sgbackups";
  public static final String NAME = PLURAL + "." + StackGresContext.CRD_GROUP;
  public static final String APIVERSION = StackGresContext.CRD_GROUP
      + "/" + StackGresContext.CRD_VERSION;

}

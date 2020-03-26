/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgbackup;

import io.stackgres.operator.common.StackGresUtil;

public enum StackGresBackupDefinition {

  ;

  public static final String KIND = "SGBackup";
  public static final String SINGULAR = "sgbackup";
  public static final String PLURAL = "sgbackups";
  public static final String NAME = PLURAL + "." + StackGresUtil.CRD_GROUP;
  public static final String APIVERSION = StackGresUtil.CRD_GROUP + "/" + StackGresUtil.CRD_VERSION;

}

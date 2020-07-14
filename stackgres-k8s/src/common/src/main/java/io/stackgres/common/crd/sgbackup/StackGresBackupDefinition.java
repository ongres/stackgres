/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgbackup;

import io.stackgres.common.StackGresProperty;

public enum StackGresBackupDefinition {

  ;

  public static final String KIND = "SGBackup";
  public static final String SINGULAR = "sgbackup";
  public static final String PLURAL = "sgbackups";
  public static final String NAME = PLURAL + "." + StackGresProperty.CRD_GROUP.getString();
  public static final String APIVERSION = StackGresProperty.CRD_GROUP.getString()
      + "/" + StackGresProperty.CRD_VERSION.getString();

}

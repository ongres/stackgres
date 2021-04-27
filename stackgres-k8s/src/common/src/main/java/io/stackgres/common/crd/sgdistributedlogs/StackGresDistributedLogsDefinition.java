/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdistributedlogs;

import io.stackgres.common.StackGresProperty;

public enum StackGresDistributedLogsDefinition {

  ;

  public static final String KIND = "SGDistributedLogs";
  public static final String SINGULAR = "sgdistributedlogs";
  public static final String PLURAL = "sgdistributedlogs";
  public static final String NAME = PLURAL + "." + StackGresProperty.CRD_GROUP.getString();
  public static final String APIVERSION = StackGresProperty.CRD_GROUP.getString()
      + "/" + StackGresProperty.CRD_VERSION.getString();

}
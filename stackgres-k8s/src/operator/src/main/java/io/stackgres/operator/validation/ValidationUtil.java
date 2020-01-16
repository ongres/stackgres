/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

public class ValidationUtil {

  public static final String VALIDATION_PATH = "/stackgres/validation";
  public static final String CLUSTER_VALIDATION_PATH = VALIDATION_PATH + "/sgcluster";
  public static final String PGCONFIG_VALIDATION_PATH = VALIDATION_PATH + "/sgpgconfig";
  public static final String CONNPOOLCONFIG_VALIDATION_PATH =
      VALIDATION_PATH + "/sgconnectionpoolingconfig";
  public static final String BACKUPCONFIG_VALIDATION_PATH = VALIDATION_PATH + "/sgbackupconfig";
  public static final String BACKUP_VALIDATION_PATH = VALIDATION_PATH + "/sgbackup";
  public static final String PROFILE_VALIDATION_PATH = VALIDATION_PATH + "/sgprofile";
  public static final String RESTORECONFIG_VALIDATION_PATH =  VALIDATION_PATH + "/sgrestoreconfig";

  private ValidationUtil() {
  }

}

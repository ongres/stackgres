/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

public interface ValidationUtil {

  String VALIDATION_PATH = "/stackgres/validation";
  String CLUSTER_VALIDATION_PATH = VALIDATION_PATH + "/sgcluster";
  String PGCONFIG_VALIDATION_PATH = VALIDATION_PATH + "/sgpgconfig";
  String CONNPOOLCONFIG_VALIDATION_PATH = VALIDATION_PATH + "/sgpoolconfig";
  String BACKUPCONFIG_VALIDATION_PATH = VALIDATION_PATH + "/sgbackupconfig";
  String BACKUP_VALIDATION_PATH = VALIDATION_PATH + "/sgbackup";
  String PROFILE_VALIDATION_PATH = VALIDATION_PATH + "/sginstanceprofile";
  String DISTRIBUTED_LOGS_VALIDATION_PATH = VALIDATION_PATH + "/sgdistributedlogs";
  String DBOPS_VALIDATION_PATH = VALIDATION_PATH + "/sgdbops";

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

public interface ConversionUtil {

  String CONVERSION_PATH = "/stackgres/conversion";
  String CLUSTER_CONVERSION_PATH = CONVERSION_PATH + "/sgcluster";
  String PGCONFIG_CONVERSION_PATH = CONVERSION_PATH + "/sgpgconfig";
  String CONNPOOLCONFIG_CONVERSION_PATH =  CONVERSION_PATH + "/sgpoolconfig";
  String BACKUPCONFIG_CONVERSION_PATH = CONVERSION_PATH + "/sgbackupconfig";
  String BACKUP_CONVERSION_PATH = CONVERSION_PATH + "/sgbackup";
  String PROFILE_CONVERSION_PATH = CONVERSION_PATH + "/sginstanceprofile";
  String DISTRIBUTED_LOGS_CONVERSION_PATH = CONVERSION_PATH + "/sgdistributedlogs";
}

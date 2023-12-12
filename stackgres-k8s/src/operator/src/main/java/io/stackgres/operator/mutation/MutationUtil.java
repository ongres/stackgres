/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

public interface MutationUtil {

  String MUTATION_PATH = "/stackgres/mutation";
  String CONFIG_MUTATION_PATH = MUTATION_PATH + "/sgconfig";
  String CLUSTER_MUTATION_PATH = MUTATION_PATH + "/sgcluster";
  String PGCONFIG_MUTATION_PATH = MUTATION_PATH + "/sgpgconfig";
  String CONNPOOLCONFIG_MUTATION_PATH =  MUTATION_PATH + "/sgpoolconfig";
  String BACKUP_MUTATION_PATH = MUTATION_PATH + "/sgbackup";
  String PROFILE_MUTATION_PATH = MUTATION_PATH + "/sginstanceprofile";
  String DISTRIBUTED_LOGS_MUTATION_PATH = MUTATION_PATH + "/sgdistributedlogs";
  String DBOPS_MUTATION_PATH = MUTATION_PATH + "/sgdbops";
  String OBJECT_STORAGE_MUTATION_PATH = MUTATION_PATH + "/sgobjectstorage";
  String SCRIPT_MUTATION_PATH = MUTATION_PATH + "/sgscript";
  String SHARDED_CLUSTER_MUTATION_PATH = MUTATION_PATH + "/sgshardedcluster";
  String SHARDED_BACKUP_MUTATION_PATH = MUTATION_PATH + "/sgshardedbackup";
  String SHARDED_DBOPS_MUTATION_PATH = MUTATION_PATH + "/sgshardeddbops";

}

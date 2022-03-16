/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresVersion;

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
  String OBJECTSTORAGE_VALIDATION_PATH = VALIDATION_PATH + "/sgobjectstorage";

  Map<StackGresComponent, Map<StackGresVersion, List<String>>> SUPPORTED_POSTGRES_VERSIONS
      = ImmutableList.of(
          StackGresComponent.POSTGRESQL,
          StackGresComponent.BABELFISH
          )
          .stream()
          .collect(ImmutableMap.toImmutableMap(Function.identity(),
              component -> component.getComponentVersions()
              .entrySet()
              .stream()
              .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey,
                  entry -> entry.getValue().getOrderedVersions().toList()))));
}

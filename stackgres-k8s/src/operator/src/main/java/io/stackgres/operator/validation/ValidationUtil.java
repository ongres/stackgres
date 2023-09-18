/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.component.Component.ImageVersion;

public interface ValidationUtil {

  String VALIDATION_PATH = "/stackgres/validation";
  String CONFIG_VALIDATION_PATH = VALIDATION_PATH + "/sgconfig";
  String CLUSTER_VALIDATION_PATH = VALIDATION_PATH + "/sgcluster";
  String PGCONFIG_VALIDATION_PATH = VALIDATION_PATH + "/sgpgconfig";
  String CONNPOOLCONFIG_VALIDATION_PATH = VALIDATION_PATH + "/sgpoolconfig";
  String BACKUPCONFIG_VALIDATION_PATH = VALIDATION_PATH + "/sgbackupconfig";
  String BACKUP_VALIDATION_PATH = VALIDATION_PATH + "/sgbackup";
  String PROFILE_VALIDATION_PATH = VALIDATION_PATH + "/sginstanceprofile";
  String DISTRIBUTED_LOGS_VALIDATION_PATH = VALIDATION_PATH + "/sgdistributedlogs";
  String DBOPS_VALIDATION_PATH = VALIDATION_PATH + "/sgdbops";
  String OBJECT_STORAGE_VALIDATION_PATH = VALIDATION_PATH + "/sgobjectstorage";
  String SCRIPT_VALIDATION_PATH = VALIDATION_PATH + "/sgscript";
  String SHARDED_CLUSTER_VALIDATION_PATH = VALIDATION_PATH + "/sgshardedcluster";

  Map<StackGresComponent, Map<StackGresVersion, List<String>>> SUPPORTED_POSTGRES_VERSIONS =
      Stream.of(StackGresComponent.POSTGRESQL, StackGresComponent.BABELFISH)
          .collect(ImmutableMap.toImmutableMap(Function.identity(),
              component -> component.getComponentVersions()
                  .entrySet()
                  .stream()
                  .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey,
                      entry -> entry.getValue().streamOrderedVersions().toList()))));

  Map<StackGresComponent, Map<StackGresVersion, List<String>>>
      SUPPORTED_SHARDED_CLUSTER_POSTGRES_VERSIONS =
      Stream.of(StackGresComponent.POSTGRESQL, StackGresComponent.BABELFISH)
          .collect(ImmutableMap.toImmutableMap(Function.identity(),
              component -> component.getComponentVersions()
                  .entrySet()
                  .stream()
                  .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey,
                      entry -> entry.getValue().streamOrderedTagVersions()
                      .limitWhileClosed(imageVersion -> imageVersion.getBuild().equals("6.22"))
                      .map(ImageVersion::getVersion)
                      .grouped(Function.identity())
                      .map(t -> t.v1)
                      .toList()))));
}

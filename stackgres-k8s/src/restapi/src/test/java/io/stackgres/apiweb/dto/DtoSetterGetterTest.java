/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto;

import io.stackgres.apiweb.dto.backup.BackupDto;
import io.stackgres.apiweb.dto.cluster.ClusterDto;
import io.stackgres.apiweb.dto.clusterrole.ClusterRoleDto;
import io.stackgres.apiweb.dto.clusterrolebinding.ClusterRoleBindingDto;
import io.stackgres.apiweb.dto.config.ConfigDto;
import io.stackgres.apiweb.dto.configmap.ConfigMapDto;
import io.stackgres.apiweb.dto.dbops.DbOpsDto;
import io.stackgres.apiweb.dto.distributedlogs.DistributedLogsDto;
import io.stackgres.apiweb.dto.event.EventDto;
import io.stackgres.apiweb.dto.extension.ExtensionsDto;
import io.stackgres.apiweb.dto.objectstorage.ObjectStorageDto;
import io.stackgres.apiweb.dto.pgconfig.PostgresConfigDto;
import io.stackgres.apiweb.dto.pooling.PoolingConfigDto;
import io.stackgres.apiweb.dto.profile.ProfileDto;
import io.stackgres.apiweb.dto.role.RoleDto;
import io.stackgres.apiweb.dto.rolebinding.RoleBindingDto;
import io.stackgres.apiweb.dto.script.ScriptDto;
import io.stackgres.apiweb.dto.secret.SecretDto;
import io.stackgres.apiweb.dto.shardedbackup.ShardedBackupDto;
import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterDto;
import io.stackgres.apiweb.dto.shardeddbops.ShardedDbOpsDto;
import io.stackgres.apiweb.dto.user.UserDto;
import io.stackgres.testutil.ModelTestUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DtoSetterGetterTest {

  @ParameterizedTest
  @ValueSource(classes = {
      ConfigDto.class,
      ClusterDto.class,
      ProfileDto.class,
      PostgresConfigDto.class,
      PoolingConfigDto.class,
      ObjectStorageDto.class,
      BackupDto.class,
      DbOpsDto.class,
      DistributedLogsDto.class,
      ScriptDto.class,
      ConfigMapDto.class,
      EventDto.class,
      ExtensionsDto.class,
      SecretDto.class,
      ShardedClusterDto.class,
      ShardedBackupDto.class,
      ShardedDbOpsDto.class,
      UserDto.class,
      ClusterRoleDto.class,
      ClusterRoleBindingDto.class,
      RoleDto.class,
      RoleBindingDto.class,
  })
  void assertSettersAndGetters(Class<?> sourceClazz) {
    ModelTestUtil.assertSettersAndGetters(sourceClazz);
  }

}

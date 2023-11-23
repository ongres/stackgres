/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto;

import io.stackgres.apiweb.dto.cluster.ClusterDto;
import io.stackgres.apiweb.dto.configmap.ConfigMapDto;
import io.stackgres.apiweb.dto.dbops.DbOpsDto;
import io.stackgres.apiweb.dto.distributedlogs.DistributedLogsDto;
import io.stackgres.apiweb.dto.event.EventDto;
import io.stackgres.apiweb.dto.extension.ExtensionsDto;
import io.stackgres.apiweb.dto.objectstorage.ObjectStorageDto;
import io.stackgres.apiweb.dto.pgconfig.PostgresConfigDto;
import io.stackgres.apiweb.dto.pooling.PoolingConfigDto;
import io.stackgres.apiweb.dto.profile.ProfileDto;
import io.stackgres.apiweb.dto.script.ScriptDto;
import io.stackgres.apiweb.dto.secret.SecretDto;
import io.stackgres.testutil.SetterGetterTestCase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DtoSetterGetterTest extends SetterGetterTestCase {

  @ParameterizedTest
  @ValueSource(classes = {
      ClusterDto.class,
      ProfileDto.class,
      PostgresConfigDto.class,
      PoolingConfigDto.class,
      ObjectStorageDto.class,
      DbOpsDto.class,
      DistributedLogsDto.class,
      ScriptDto.class,
      ConfigMapDto.class,
      EventDto.class,
      ExtensionsDto.class,
      SecretDto.class,
  })
  @Override
  protected void assertSettersAndGetters(Class<?> sourceClazz) {
    super.assertSettersAndGetters(sourceClazz);
  }

}

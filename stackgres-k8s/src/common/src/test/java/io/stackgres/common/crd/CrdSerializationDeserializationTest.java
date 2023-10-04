/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.common.JsonMapperCustomizer;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.ModelTestUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CrdSerializationDeserializationTest {

  @ParameterizedTest
  @ValueSource(classes = {
      StackGresCluster.class,
      StackGresProfile.class,
      StackGresPostgresConfig.class,
      StackGresPoolingConfig.class,
      StackGresBackupConfig.class,
      StackGresObjectStorage.class,
      StackGresDbOps.class,
      StackGresDistributedLogs.class,
      StackGresScript.class,
      StackGresShardedCluster.class,
      StackGresShardedBackup.class,
      StackGresShardedDbOps.class,
  })
  protected void assertSerializationAndDeserialization(Class<?> sourceClazz) throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    new JsonMapperCustomizer().customize(objectMapper);
    var object = ModelTestUtil.createWithRandomData(sourceClazz);
    var jsonObject = objectMapper.valueToTree(object);
    var objectCopy = objectMapper.readValue(jsonObject.toString(), sourceClazz);
    var jsonObjectCopy = objectMapper.valueToTree(objectCopy);
    JsonUtil.assertJsonEquals(jsonObject, jsonObjectCopy);
  }

}

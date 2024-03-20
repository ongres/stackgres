/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

import io.stackgres.common.crd.external.autoscaling.VerticalPodAutoscaler;
import io.stackgres.common.crd.external.keda.ScaledObject;
import io.stackgres.common.crd.external.keda.TriggerAuthentication;
import io.stackgres.common.crd.external.prometheus.PodMonitor;
import io.stackgres.common.crd.external.prometheus.ServiceMonitor;
import io.stackgres.common.crd.external.shardingsphere.ComputeNode;
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
import io.stackgres.testutil.ModelTestUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CrdSetterGetterTest {

  @ParameterizedTest
  @ValueSource(classes = {
      StackGresCluster.class,
      StackGresProfile.class,
      StackGresPostgresConfig.class,
      StackGresPoolingConfig.class,
      StackGresObjectStorage.class,
      StackGresDbOps.class,
      StackGresDistributedLogs.class,
      StackGresScript.class,
      StackGresShardedCluster.class,
      StackGresShardedBackup.class,
      StackGresShardedDbOps.class,
      PodMonitor.class,
      ServiceMonitor.class,
      ComputeNode.class,
      ScaledObject.class,
      TriggerAuthentication.class,
      VerticalPodAutoscaler.class,
  })
  void assertSettersAndGetters(Class<?> sourceClazz) {
    ModelTestUtil.assertSettersAndGetters(sourceClazz);
  }

}

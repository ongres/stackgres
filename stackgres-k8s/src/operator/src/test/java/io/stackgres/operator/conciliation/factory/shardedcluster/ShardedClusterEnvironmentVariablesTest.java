/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.List;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedClusterEnvironmentVariablesTest {

  @Mock
  private StackGresShardedClusterContext context;

  private ShardedClusterEnvironmentVariables environmentVariables;

  private StackGresShardedCluster cluster;

  @BeforeEach
  void setUp() {
    environmentVariables = new ShardedClusterEnvironmentVariables();
    cluster = Fixtures.shardedCluster().loadDefault().get();
    when(context.getShardedCluster()).thenReturn(cluster);
  }

  @Test
  void buildEnvironmentVariables_shouldReturnEnvVars() {
    List<EnvVar> envVars = environmentVariables.buildEnvironmentVariables(context);

    assertNotNull(envVars);
  }

  @Test
  void buildEnvironmentVariables_shouldReturnNonEmptyList() {
    List<EnvVar> envVars = environmentVariables.buildEnvironmentVariables(context);

    assertFalse(envVars.isEmpty());
  }

  @Test
  void buildEnvironmentVariables_eachEnvVarShouldHaveName() {
    List<EnvVar> envVars = environmentVariables.buildEnvironmentVariables(context);

    envVars.forEach(envVar -> assertNotNull(envVar.getName(),
        "Each environment variable should have a non-null name"));
  }

}

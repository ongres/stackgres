/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.stackgres.common.ClusterEnvVar;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterEnvironmentVariablesTest {

  @Mock
  private StackGresClusterContext context;

  private StackGresCluster cluster;

  private ClusterEnvironmentVariables factory;

  @BeforeEach
  void setUp() {
    factory = new ClusterEnvironmentVariables();
    cluster = Fixtures.cluster().loadDefault().get();
    lenient().when(context.getCluster()).thenReturn(cluster);
    lenient().when(context.getResource()).thenReturn(cluster);

    Map<String, String> envVarsMap = Seq.of(ClusterEnvVar.values())
        .map(cssev -> cssev.envVar(cluster))
        .collect(Collectors.toMap(EnvVar::getName, EnvVar::getValue));
    lenient().when(context.getEnvironmentVariables()).thenReturn(envVarsMap);
  }

  @Test
  void buildEnvironmentVariables_shouldReturnNonEmptyList() {
    List<EnvVar> envVars = factory.buildEnvironmentVariables(context);

    assertFalse(envVars.isEmpty());
  }

  @Test
  void buildEnvironmentVariables_shouldContainAllClusterPathEnvVars() {
    List<EnvVar> envVars = factory.buildEnvironmentVariables(context);
    List<String> envVarNames = envVars.stream()
        .map(EnvVar::getName)
        .toList();

    for (ClusterPath clusterPath : ClusterPath.values()) {
      assertTrue(envVarNames.contains(clusterPath.name()),
          "Expected env var " + clusterPath.name() + " to be present");
    }
  }

  @Test
  void buildEnvironmentVariables_shouldContainAllClusterEnvVarValues() {
    List<EnvVar> envVars = factory.buildEnvironmentVariables(context);
    List<String> envVarNames = envVars.stream()
        .map(EnvVar::getName)
        .toList();

    for (ClusterEnvVar clusterEnvVar : ClusterEnvVar.values()) {
      assertTrue(envVarNames.contains(clusterEnvVar.name()),
          "Expected env var " + clusterEnvVar.name() + " to be present");
    }
  }

  @Test
  void buildEnvironmentVariables_shouldContainExpectedClusterPathValues() {
    List<EnvVar> envVars = factory.buildEnvironmentVariables(context);

    Optional<EnvVar> etcPasswdPath = envVars.stream()
        .filter(e -> e.getName().equals(ClusterPath.ETC_PASSWD_PATH.name()))
        .findFirst();
    assertTrue(etcPasswdPath.isPresent());
    assertEquals("/etc/passwd", etcPasswdPath.get().getValue());

    Optional<EnvVar> pgRunPath = envVars.stream()
        .filter(e -> e.getName().equals(ClusterPath.PG_RUN_PATH.name()))
        .findFirst();
    assertTrue(pgRunPath.isPresent());
    assertEquals("/var/run/postgresql", pgRunPath.get().getValue());
  }

  @Test
  void buildEnvironmentVariables_shouldHaveCorrectTotalCount() {
    List<EnvVar> envVars = factory.buildEnvironmentVariables(context);

    int expectedCount = ClusterPath.values().length + ClusterEnvVar.values().length;
    assertEquals(expectedCount, envVars.size());
  }
}

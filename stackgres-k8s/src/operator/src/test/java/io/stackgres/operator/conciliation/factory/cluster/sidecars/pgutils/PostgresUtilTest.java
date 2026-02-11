/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pgutils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.util.List;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.PostgresSocketMounts;
import io.stackgres.operator.conciliation.factory.UserOverrideMounts;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.ImmutableClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.PostgresEnvironmentVariables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostgresUtilTest {

  @Mock
  private PostgresEnvironmentVariables postgresEnvironmentVariables;

  @Mock
  private PostgresSocketMounts postgresSocketMounts;

  @Mock
  private UserOverrideMounts userOverrideMounts;

  private PostgresUtil postgresUtil;

  @BeforeEach
  void setUp() {
    lenient().when(postgresEnvironmentVariables.getEnvVars(any()))
        .thenReturn(List.of(
            new EnvVarBuilder()
                .withName("PGUSER")
                .withValue("postgres")
                .build(),
            new EnvVarBuilder()
                .withName("PGDATABASE")
                .withValue("postgres")
                .build()));

    postgresUtil = new PostgresUtil(
        postgresEnvironmentVariables, postgresSocketMounts, userOverrideMounts);
  }

  @Test
  void isActivated_whenNotDisabled_shouldBeActivated() {
    ClusterContainerContext context = getClusterContainerContext();
    // Default fixture has disablePostgresUtil: false

    Assertions.assertTrue(postgresUtil.isActivated(context));
  }

  @Test
  void isActivated_whenExplicitlyNotDisabled_shouldBeActivated() {
    ClusterContainerContext context = getClusterContainerContext();
    context.getClusterContext().getCluster().getSpec().getPods().setDisablePostgresUtil(false);

    Assertions.assertTrue(postgresUtil.isActivated(context));
  }

  @Test
  void isActivated_whenDisabled_shouldNotBeActivated() {
    ClusterContainerContext context = getClusterContainerContext();
    context.getClusterContext().getCluster().getSpec().getPods().setDisablePostgresUtil(true);

    Assertions.assertFalse(postgresUtil.isActivated(context));
  }

  @Test
  void getContainer_shouldHaveCorrectEnvVars() {
    ClusterContainerContext context = getClusterContainerContext();

    Container container = postgresUtil.getContainer(context);

    Assertions.assertEquals(
        StackGresContainer.POSTGRES_UTIL.getName(),
        container.getName());

    Assertions.assertTrue(
        container.getEnv().stream()
            .map(EnvVar::getName)
            .anyMatch(name -> "PGUSER".equals(name)),
        "Should contain PGUSER env var");
    Assertions.assertTrue(
        container.getEnv().stream()
            .map(EnvVar::getName)
            .anyMatch(name -> "PGDATABASE".equals(name)),
        "Should contain PGDATABASE env var");
  }

  @Test
  void getContainer_shouldUseShellCommand() {
    ClusterContainerContext context = getClusterContainerContext();

    Container container = postgresUtil.getContainer(context);

    Assertions.assertNotNull(container.getCommand());
    Assertions.assertEquals(List.of("/bin/sh"), container.getCommand());
    Assertions.assertNotNull(container.getArgs());
    Assertions.assertEquals(List.of("-c", "while true; do sleep 10; done"), container.getArgs());
  }

  @Test
  void getContainer_shouldHaveStdinAndTtyEnabled() {
    ClusterContainerContext context = getClusterContainerContext();

    Container container = postgresUtil.getContainer(context);

    Assertions.assertEquals(Boolean.TRUE, container.getStdin());
    Assertions.assertEquals(Boolean.TRUE, container.getTty());
  }

  private ClusterContainerContext getClusterContainerContext() {
    return ImmutableClusterContainerContext.builder()
        .clusterContext(StackGresClusterContext.builder()
            .config(getDefaultConfig())
            .source(getDefaultCluster())
            .postgresConfig(new StackGresPostgresConfig())
            .profile(new StackGresProfile())
            .currentInstances(0)
            .build())
        .dataVolumeName("test")
        .build();
  }

  private StackGresConfig getDefaultConfig() {
    return Fixtures.config().loadDefault().get();
  }

  private StackGresCluster getDefaultCluster() {
    return Fixtures.cluster().loadDefault().get();
  }

}

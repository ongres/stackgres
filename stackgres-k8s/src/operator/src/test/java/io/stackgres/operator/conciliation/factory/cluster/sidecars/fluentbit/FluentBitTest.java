/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.fluentbit;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.PostgresSocketMounts;
import io.stackgres.operator.conciliation.factory.TemplatesMounts;
import io.stackgres.operator.conciliation.factory.UserOverrideMounts;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.ImmutableClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.LogVolumeMounts;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FluentBitTest {

  @Mock
  private LabelFactoryForCluster labelFactory;

  @Mock
  private LogVolumeMounts logMounts;

  @Mock
  private PostgresSocketMounts postgresSocket;

  @Mock
  private TemplatesMounts templatesMounts;

  @Mock
  private UserOverrideMounts userOverrideMounts;

  private FluentBit fluentBit;

  @BeforeEach
  void setUp() {
    fluentBit = new FluentBit(
        labelFactory, logMounts, postgresSocket, templatesMounts, userOverrideMounts);
  }

  @Test
  void isActivated_whenDistributedLogsConfigured_shouldBeActivated() {
    ClusterContainerContext context = getClusterContainerContext();
    // Default fixture has distributedLogs configured with sgDistributedLogs: "distributedlogs"

    Assertions.assertTrue(fluentBit.isActivated(context));
  }

  @Test
  void isActivated_whenDistributedLogsNotConfigured_shouldNotBeActivated() {
    ClusterContainerContext context = getClusterContainerContextWithoutDistributedLogs();

    Assertions.assertFalse(fluentBit.isActivated(context));
  }

  @Test
  void isActivated_whenDistributedLogsSetToNull_shouldNotBeActivated() {
    ClusterContainerContext context = getClusterContainerContext();
    context.getClusterContext().getSource().getSpec().setDistributedLogs(null);

    Assertions.assertFalse(fluentBit.isActivated(context));
  }

  @Test
  void getContainer_shouldHaveCorrectVolumeMounts() {
    ClusterContainerContext context = getClusterContainerContext();

    Container container = fluentBit.getContainer(context);

    Assertions.assertEquals(
        StackGresContainer.FLUENT_BIT.getName(),
        container.getName());
    Assertions.assertTrue(
        container.getVolumeMounts().stream()
            .map(VolumeMount::getName)
            .anyMatch(name -> name.equals(StackGresVolume.FLUENT_BIT.getName())),
        "Should contain fluent-bit config volume mount");
    Assertions.assertTrue(
        container.getVolumeMounts().stream()
            .anyMatch(vm -> vm.getMountPath().equals("/etc/fluent-bit")
                && Boolean.TRUE.equals(vm.getReadOnly())),
        "Should mount fluent-bit config as read-only at /etc/fluent-bit");
  }

  @Test
  void getContainer_shouldHaveCorrectCommand() {
    ClusterContainerContext context = getClusterContainerContext();

    Container container = fluentBit.getContainer(context);

    Assertions.assertNotNull(container.getCommand());
    Assertions.assertFalse(container.getCommand().isEmpty());
    Assertions.assertEquals("/bin/sh", container.getCommand().get(0));
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

  private ClusterContainerContext getClusterContainerContextWithoutDistributedLogs() {
    return ImmutableClusterContainerContext.builder()
        .clusterContext(StackGresClusterContext.builder()
            .config(getDefaultConfig())
            .source(getClusterWithoutDistributedLogs())
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

  private StackGresCluster getClusterWithoutDistributedLogs() {
    return Fixtures.cluster().loadWithoutDistributedLogs().get();
  }

}

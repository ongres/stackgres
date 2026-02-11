/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresPostgresFlavor;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.LocalBinMounts;
import io.stackgres.operator.conciliation.factory.PostgresSocketMounts;
import io.stackgres.operator.conciliation.factory.TemplatesMounts;
import io.stackgres.operator.conciliation.factory.UserOverrideMounts;
import io.stackgres.operator.conciliation.factory.cluster.BackupMounts;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.HugePagesMounts;
import io.stackgres.operator.conciliation.factory.cluster.PostgresEnvironmentVariables;
import io.stackgres.operator.conciliation.factory.cluster.PostgresExtensionMounts;
import io.stackgres.operator.conciliation.factory.cluster.ReplicateMounts;
import io.stackgres.operator.conciliation.factory.cluster.ReplicationInitializationMounts;
import io.stackgres.operator.conciliation.factory.cluster.RestoreMounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatroniTest {

  private static final String POSTGRES_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions().findFirst().get();

  @Mock
  PatroniEnvironmentVariables patroniEnvironmentVariables;
  @Mock
  PostgresEnvironmentVariables postgresEnvironmentVariables;
  @Mock
  PostgresSocketMounts postgresSocket;
  @Mock
  PostgresExtensionMounts postgresExtensions;
  @Mock
  TemplatesMounts templateMounts;
  @Mock
  UserOverrideMounts userOverrideMounts;
  @Mock
  LocalBinMounts localBinMounts;
  @Mock
  RestoreMounts restoreMounts;
  @Mock
  BackupMounts backupMounts;
  @Mock
  ReplicationInitializationMounts replicationInitializationMounts;
  @Mock
  ReplicateMounts replicateMounts;
  @Mock
  PatroniMounts patroniMounts;
  @Mock
  HugePagesMounts hugePagesMounts;
  @Mock
  PatroniConfigMap patroniConfigMap;

  private Patroni patroni;

  @Mock
  private ClusterContainerContext clusterContainerContext;

  @Mock(answer = Answers.CALLS_REAL_METHODS)
  private StackGresClusterContext clusterContext;

  @Mock
  private ResourceRequirements podResources;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    patroni = new Patroni(patroniEnvironmentVariables, postgresEnvironmentVariables,
        postgresSocket, postgresExtensions, templateMounts, userOverrideMounts, localBinMounts,
        restoreMounts, backupMounts, replicationInitializationMounts,
        replicateMounts, patroniMounts, hugePagesMounts, patroniConfigMap);
    cluster = Fixtures.cluster().loadDefault().get();
    cluster.getSpec().getPostgres().setVersion(POSTGRES_VERSION);
    when(clusterContainerContext.getClusterContext()).thenReturn(clusterContext);
    when(patroniEnvironmentVariables.getEnvVars(clusterContext)).thenReturn(List.of());
    when(postgresEnvironmentVariables.getEnvVars(clusterContext)).thenReturn(List.of());
    when(patroniConfigMap.buildSource(clusterContext))
        .thenReturn(new ConfigMapBuilder()
                .withData(Map.of(StackGresUtil.MD5SUM_KEY, "test"))
                .build());
    when(clusterContext.getSource()).thenReturn(cluster);
    when(clusterContext.getCluster()).thenReturn(cluster);
  }

  @Test
  void givenACluster_itShouldGetVolumeMounts() {
    Container patroniContainer = patroni.getContainer(clusterContainerContext);
    var dshmVolumeMount = new VolumeMountBuilder()
        .withName(StackGresVolume.DSHM.getName())
        .withMountPath(ClusterPath.SHARED_MEMORY_PATH.path())
        .build();
    var pgLogVolumeMount = new VolumeMountBuilder()
        .withName(StackGresVolume.LOG.getName())
        .withMountPath(ClusterPath.PG_LOG_PATH.path())
        .build();
    assertTrue(patroniContainer.getVolumeMounts().contains(dshmVolumeMount));
    assertTrue(patroniContainer.getVolumeMounts().contains(pgLogVolumeMount));

    verify(postgresSocket, times(1)).getVolumeMounts(any());
    verify(localBinMounts, times(1)).getVolumeMounts(any());
    verify(patroniMounts, times(1)).getVolumeMounts(any());
    verify(backupMounts, times(1)).getVolumeMounts(any());
    verify(replicateMounts, times(1)).getVolumeMounts(any());
    verify(postgresExtensions, times(1)).getVolumeMounts(any());
    verify(hugePagesMounts, times(1)).getVolumeMounts(any());
  }

  @Test
  void givenACluster_itShouldGetEnvVars() {
    patroni.getContainer(clusterContainerContext);

    verify(localBinMounts, times(1)).getDerivedEnvVars(any());
    verify(patroniMounts, times(1)).getDerivedEnvVars(any());
    verify(backupMounts, times(1)).getDerivedEnvVars(any());
    verify(replicateMounts, times(1)).getDerivedEnvVars(any());
    verify(postgresExtensions, times(1)).getDerivedEnvVars(any());
    verify(hugePagesMounts, times(1)).getDerivedEnvVars(any());
    verify(patroniEnvironmentVariables, times(1)).getEnvVars(any());
    verify(postgresEnvironmentVariables, times(1)).getEnvVars(any());
    verify(restoreMounts, times(1)).getDerivedEnvVars(any());
  }

  @Test
  void givenACluster_itShouldGetPortsAndCommands() {
    Container patroniContainer = patroni.getContainer(clusterContainerContext);
    assertTrue(patroniContainer.getCommand().contains("/bin/sh"));
    assertTrue(patroniContainer.getPorts().isEmpty());
  }

  @Test
  void getContainer_whenEnvoyDisabled_shouldUseDirectPort() {
    cluster.getSpec().getPods().setDisableEnvoy(true);

    Container patroniContainer = patroni.getContainer(clusterContainerContext);

    assertFalse(patroniContainer.getPorts().isEmpty(),
        "When envoy is disabled, ports should be exposed directly on the container");
    assertTrue(patroniContainer.getPorts().stream()
            .anyMatch(port -> port.getContainerPort() == EnvoyUtil.PATRONI_PORT),
        "When envoy is disabled, Patroni port should be exposed directly");
    assertTrue(patroniContainer.getReadinessProbe().getHttpGet()
            .getPort().getIntVal() == EnvoyUtil.PATRONI_PORT,
        "When envoy is disabled, readiness probe should use the direct Patroni port");
  }

  @Test
  void getContainer_whenBabelfishFlavor_shouldHaveBabelfishPorts() {
    cluster.getSpec().getPods().setDisableEnvoy(true);
    cluster.getSpec().getPostgres().setVersion(
        StackGresComponent.BABELFISH.getLatest().streamOrderedVersions().findFirst().get());
    cluster.getSpec().getPostgres().setFlavor(StackGresPostgresFlavor.BABELFISH.toString());
    lenient().when(patroniEnvironmentVariables.getEnvVars(clusterContext)).thenReturn(List.of());
    lenient().when(postgresEnvironmentVariables.getEnvVars(clusterContext)).thenReturn(List.of());

    Container patroniContainer = patroni.getContainer(clusterContainerContext);

    List<ContainerPort> ports = patroniContainer.getPorts();
    assertFalse(ports.isEmpty(),
        "When Babelfish flavor with envoy disabled, container should have ports");
    assertTrue(ports.stream()
            .anyMatch(port -> port.getName().equals(EnvoyUtil.BABELFISH_PORT_NAME)),
        "When Babelfish flavor, container should have a Babelfish port");
    assertTrue(ports.stream()
            .anyMatch(port -> port.getContainerPort() == EnvoyUtil.BF_PORT),
        "When Babelfish flavor, container should expose the Babelfish port number");
  }
}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.LocalBinMounts;
import io.stackgres.operator.conciliation.factory.PostgresSocketMount;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.cluster.BackupVolumeMounts;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.HugePagesMounts;
import io.stackgres.operator.conciliation.factory.cluster.PostgresExtensionMounts;
import io.stackgres.operator.conciliation.factory.cluster.ReplicateVolumeMounts;
import io.stackgres.operator.conciliation.factory.cluster.ReplicationInitializationVolumeMounts;
import io.stackgres.operator.conciliation.factory.cluster.RestoreVolumeMounts;
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
  ResourceFactory<StackGresClusterContext, List<EnvVar>> patroniEnvironmentVariables;
  @Mock
  PostgresSocketMount postgresSocket;
  @Mock
  PostgresExtensionMounts postgresExtensions;
  @Mock
  LocalBinMounts localBinMounts;
  @Mock
  RestoreVolumeMounts restoreMounts;
  @Mock
  BackupVolumeMounts backupMounts;
  @Mock
  ReplicationInitializationVolumeMounts replicationInitializationVolumeMounts;
  @Mock
  ReplicateVolumeMounts replicateMounts;
  @Mock
  PatroniVolumeMounts patroniMounts;
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
    patroni = new Patroni(patroniEnvironmentVariables,
        postgresSocket, postgresExtensions, localBinMounts, restoreMounts, backupMounts,
        replicationInitializationVolumeMounts,
        replicateMounts, patroniMounts, hugePagesMounts, patroniConfigMap);
    cluster = Fixtures.cluster().loadDefault().get();
    cluster.getSpec().getPostgres().setVersion(POSTGRES_VERSION);
    when(clusterContainerContext.getClusterContext()).thenReturn(clusterContext);
    when(patroniEnvironmentVariables.createResource(clusterContext)).thenReturn(List.of());
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
    verify(patroniEnvironmentVariables, times(1)).createResource(any());
    verify(restoreMounts, times(1)).getDerivedEnvVars(any());
  }

  @Test
  void givenACluster_itShouldGetPortsAndCommands() {
    Container patroniContainer = patroni.getContainer(clusterContainerContext);
    assertTrue(patroniContainer.getCommand().contains("/bin/sh"));
    assertTrue(patroniContainer.getPorts().stream()
        .filter(p -> p.getName().equals(EnvoyUtil.POSTGRES_PORT_NAME))
        .findFirst().isPresent());
    assertTrue(patroniContainer.getPorts().stream()
        .filter(p -> p.getName().equals(EnvoyUtil.POSTGRES_REPLICATION_PORT_NAME))
        .findFirst().isPresent());
    assertTrue(patroniContainer.getPorts().stream()
        .filter(p -> p.getName().equals(EnvoyUtil.PATRONI_RESTAPI_PORT_NAME))
        .findFirst().isPresent());
  }
}

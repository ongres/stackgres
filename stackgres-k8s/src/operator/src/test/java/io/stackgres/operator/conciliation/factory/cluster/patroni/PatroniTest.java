/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.LocalBinMounts;
import io.stackgres.operator.conciliation.factory.PostgresSocketMount;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.VolumeDiscoverer;
import io.stackgres.operator.conciliation.factory.cluster.BackupVolumeMounts;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.HugePagesMounts;
import io.stackgres.operator.conciliation.factory.cluster.PostgresExtensionMounts;
import io.stackgres.operator.conciliation.factory.cluster.RestoreVolumeMounts;
import io.stackgres.operator.conciliation.factory.cluster.StatefulSetDynamicVolumes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatroniTest {

  private static final String POSTGRES_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions().findFirst().get();

  @Mock
  ResourceFactory<StackGresClusterContext, List<EnvVar>> patroniEnvironmentVariables;
  @Mock
  ResourceFactory<StackGresClusterContext, ResourceRequirements> requirementsFactory;
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
  HugePagesMounts hugePagesMounts;

  @Mock
  VolumeDiscoverer<StackGresClusterContext> volumeDiscoverer;

  private Patroni patroni;

  @Mock
  private ClusterContainerContext clusterContainerContext;

  @Mock
  private StackGresClusterContext clusterContext;

  @Mock
  private ResourceRequirements podResources;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    patroni = new Patroni(patroniEnvironmentVariables, requirementsFactory,
        postgresSocket, postgresExtensions, localBinMounts, restoreMounts, backupMounts,
        hugePagesMounts, volumeDiscoverer);
    cluster = Fixtures.cluster().loadDefault().get();
    cluster.getSpec().getPostgres().setVersion(POSTGRES_VERSION);
    when(clusterContainerContext.getClusterContext()).thenReturn(clusterContext);
    when(requirementsFactory.createResource(clusterContext)).thenReturn(podResources);
    when(patroniEnvironmentVariables.createResource(clusterContext)).thenReturn(List.of());
    when(volumeDiscoverer.discoverVolumes(clusterContext))
        .thenReturn(Map.of(StatefulSetDynamicVolumes.PATRONI_ENV.getVolumeName(),
            ImmutableVolumePair.builder()
            .volume(new VolumeBuilder()
                .withNewConfigMap()
                .withName("test")
                .endConfigMap()
                .build())
            .source(new ConfigMapBuilder()
                .withData(Map.of(StackGresUtil.MD5SUM_KEY, "test"))
                .build())
            .build()));
  }

  @Test
  void givenACluster_itShouldGetHugePagesMountsAndEnvVars() {
    when(clusterContext.getSource()).thenReturn(cluster);
    when(clusterContext.getCluster()).thenReturn(cluster);

    patroni.getContainer(clusterContainerContext);

    verify(hugePagesMounts, times(1)).getVolumeMounts(any());
    verify(hugePagesMounts, times(1)).getDerivedEnvVars(any());
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileHugePages;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.PatroniStaticVolume;
import io.stackgres.operator.conciliation.factory.PostgresContainerContext;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.VolumeDiscoverer;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import io.stackgres.operator.conciliation.factory.cluster.StackGresClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.StatefulSetDynamicVolumes;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatroniTest {

  private static final String POSTGRES_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().getOrderedVersions().findFirst().get();

  @Mock
  ResourceFactory<StackGresClusterContext, List<EnvVar>> patroniEnvironmentVariables;
  @Mock
  ResourceFactory<StackGresClusterContext, ResourceRequirements> requirementsFactory;
  @Mock
  LabelFactoryForCluster<StackGresCluster> labelFactory;
  @Mock
  VolumeMountsProvider<ContainerContext> postgresSocket;
  @Mock
  VolumeMountsProvider<PostgresContainerContext> postgresExtensions;
  @Mock
  VolumeMountsProvider<ContainerContext> localBinMounts;
  @Mock
  VolumeMountsProvider<ContainerContext> restoreMounts;
  @Mock
  VolumeMountsProvider<ContainerContext> backupMounts;
  @Mock
  VolumeDiscoverer<StackGresClusterContext> volumeDiscoverer;

  private Patroni patroni;

  @Mock
  private StackGresClusterContainerContext clusterContainerContext;

  @Mock
  private StackGresClusterContext clusterContext;

  @Mock
  private ResourceRequirements podResources;

  private StackGresCluster cluster;

  private StackGresProfile profile;

  @BeforeEach
  void setUp() {
    patroni = new Patroni(patroniEnvironmentVariables, requirementsFactory, labelFactory,
        postgresSocket, postgresExtensions, localBinMounts, restoreMounts, backupMounts,
        volumeDiscoverer);
    profile = JsonUtil.readFromJson("stackgres_profiles/size-s.json", StackGresProfile.class);
    cluster = JsonUtil.readFromJson("stackgres_cluster/default.json", StackGresCluster.class);
    cluster.getSpec().getPostgres().setVersion(POSTGRES_VERSION);
    when(clusterContainerContext.getClusterContext()).thenReturn(clusterContext);
    when(requirementsFactory.createResource(clusterContext)).thenReturn(podResources);
    when(clusterContainerContext.getDataVolumeName()).thenReturn("test");
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
  void givenAClusterWithAProfileWithHugePages_itShouldCreateTheContainerWithHugePages() {
    when(clusterContext.getSource()).thenReturn(cluster);
    when(clusterContext.getCluster()).thenReturn(cluster);
    profile.getSpec().setHugePages(new StackGresProfileHugePages());
    profile.getSpec().getHugePages().setHugepages2Mi("2Mi");
    profile.getSpec().getHugePages().setHugepages1Gi("1Gi");
    when(clusterContext.getStackGresProfile()).thenReturn(profile);

    var container = patroni.getContainer(clusterContainerContext);

    assertTrue(container.getVolumeMounts().stream()
        .anyMatch(volumeMount -> volumeMount.getName()
            .equals(PatroniStaticVolume.HUGEPAGES_2M.getVolumeName())));
    assertTrue(container.getVolumeMounts().stream()
        .filter(volumeMount -> volumeMount.getName()
            .equals(PatroniStaticVolume.HUGEPAGES_2M.getVolumeName()))
        .anyMatch(volumeMount -> volumeMount.getMountPath()
            .equals(ClusterStatefulSetPath.HUGEPAGES_2M_PATH.path())));
    assertTrue(container.getVolumeMounts().stream()
        .anyMatch(volumeMount -> volumeMount.getName()
            .equals(PatroniStaticVolume.HUGEPAGES_1G.getVolumeName())));
    assertTrue(container.getVolumeMounts().stream()
        .filter(volumeMount -> volumeMount.getName()
            .equals(PatroniStaticVolume.HUGEPAGES_1G.getVolumeName()))
        .anyMatch(volumeMount -> volumeMount.getMountPath()
            .equals(ClusterStatefulSetPath.HUGEPAGES_1G_PATH.path())));
  }

}

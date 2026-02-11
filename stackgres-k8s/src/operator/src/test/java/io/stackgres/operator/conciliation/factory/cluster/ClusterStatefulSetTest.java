/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ClusterLabelFactory;
import io.stackgres.common.labels.ClusterLabelMapper;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ImmutablePodTemplateResult;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.PodTemplateFactory;
import io.stackgres.operator.conciliation.factory.PodTemplateResult;
import io.stackgres.operator.conciliation.factory.VolumeDiscoverer;
import io.stackgres.operator.conciliation.factory.VolumePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterStatefulSetTest {

  private final LabelFactoryForCluster labelFactory =
      new ClusterLabelFactory(new ClusterLabelMapper());

  @Mock
  private PodTemplateFactoryDiscoverer<ClusterContainerContext> podTemplateSpecFactoryDiscoverer;

  @Mock
  private VolumeDiscoverer<StackGresClusterContext> volumeDiscoverer;

  @Mock
  private PodTemplateFactory<ClusterContainerContext> podTemplateFactory;

  @Mock
  private StackGresClusterContext context;

  private ClusterStatefulSet clusterStatefulSet;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    clusterStatefulSet = new ClusterStatefulSet(
        labelFactory, podTemplateSpecFactoryDiscoverer, volumeDiscoverer);
    cluster = Fixtures.cluster().loadDefault().get();
    lenient().when(context.getSource()).thenReturn(cluster);
    lenient().when(context.getCluster()).thenReturn(cluster);
    lenient().when(context.getRestoreBackup()).thenReturn(Optional.empty());
    lenient().when(context.getReplicationInitializationBackup()).thenReturn(Optional.empty());
    lenient().when(context.getCurrentInstances()).thenReturn(1);

    PodTemplateSpec podTemplateSpec = new PodTemplateSpecBuilder()
        .withNewMetadata()
        .endMetadata()
        .withNewSpec()
        .endSpec()
        .build();
    PodTemplateResult podTemplateResult = ImmutablePodTemplateResult.builder()
        .spec(podTemplateSpec)
        .claimedVolumes(List.of())
        .build();

    lenient().when(volumeDiscoverer.discoverVolumes(any())).thenReturn(Map.of());
    lenient().when(podTemplateSpecFactoryDiscoverer.discoverPodSpecFactory(any()))
        .thenReturn(podTemplateFactory);
    lenient().when(podTemplateFactory.getPodTemplateSpec(any())).thenReturn(podTemplateResult);
  }

  @Test
  void generateResource_withDefaultCluster_shouldGenerateStatefulSet() {
    List<HasMetadata> resources = clusterStatefulSet.generateResource(context).toList();

    assertFalse(resources.isEmpty());
    assertTrue(resources.getFirst() instanceof StatefulSet);
  }

  @Test
  void generateResource_withDefaultCluster_shouldHaveCorrectNameAndNamespace() {
    List<HasMetadata> resources = clusterStatefulSet.generateResource(context).toList();

    StatefulSet sts = (StatefulSet) resources.getFirst();
    assertEquals(cluster.getMetadata().getName(), sts.getMetadata().getName());
    assertEquals(cluster.getMetadata().getNamespace(), sts.getMetadata().getNamespace());
  }

  @Test
  void generateResource_withDefaultCluster_shouldHaveCorrectReplicaCount() {
    List<HasMetadata> resources = clusterStatefulSet.generateResource(context).toList();

    StatefulSet sts = (StatefulSet) resources.getFirst();
    assertEquals(cluster.getSpec().getInstances(), sts.getSpec().getReplicas());
  }

  @Test
  void generateResource_withMultipleInstances_shouldReflectInstanceCount() {
    cluster.getSpec().setInstances(3);

    List<HasMetadata> resources = clusterStatefulSet.generateResource(context).toList();

    StatefulSet sts = (StatefulSet) resources.getFirst();
    assertEquals(3, sts.getSpec().getReplicas());
  }

  @Test
  void generateResource_withDefaultCluster_shouldHaveLabelsApplied() {
    Map<String, String> expectedLabels = labelFactory.clusterLabels(cluster);

    List<HasMetadata> resources = clusterStatefulSet.generateResource(context).toList();

    StatefulSet sts = (StatefulSet) resources.getFirst();
    assertNotNull(sts.getMetadata().getLabels());
    assertEquals(expectedLabels, sts.getMetadata().getLabels());
  }

  @Test
  void generateResource_withDefaultCluster_shouldHavePodMatchLabels() {
    Map<String, String> expectedPodLabels = labelFactory.statefulSetPodLabels(cluster);

    List<HasMetadata> resources = clusterStatefulSet.generateResource(context).toList();

    StatefulSet sts = (StatefulSet) resources.getFirst();
    assertNotNull(sts.getSpec().getSelector());
    assertEquals(expectedPodLabels, sts.getSpec().getSelector().getMatchLabels());
  }

  @Test
  void generateResource_withDefaultCluster_shouldHaveOnDeleteUpdateStrategy() {
    List<HasMetadata> resources = clusterStatefulSet.generateResource(context).toList();

    StatefulSet sts = (StatefulSet) resources.getFirst();
    assertEquals("OnDelete", sts.getSpec().getUpdateStrategy().getType());
  }

  @Test
  void generateResource_withDefaultCluster_shouldHaveVolumeClaimTemplateWithCorrectStorageClass() {
    List<HasMetadata> resources = clusterStatefulSet.generateResource(context).toList();

    StatefulSet sts = (StatefulSet) resources.getFirst();
    assertNotNull(sts.getSpec().getVolumeClaimTemplates());
    assertFalse(sts.getSpec().getVolumeClaimTemplates().isEmpty());
    assertEquals("standard",
        sts.getSpec().getVolumeClaimTemplates().getFirst()
            .getSpec().getStorageClassName());
  }

  @Test
  void generateResource_withDefaultCluster_shouldHaveVolumeClaimTemplateWithCorrectSize() {
    List<HasMetadata> resources = clusterStatefulSet.generateResource(context).toList();

    StatefulSet sts = (StatefulSet) resources.getFirst();
    assertEquals("5Gi",
        sts.getSpec().getVolumeClaimTemplates().getFirst()
            .getSpec().getResources().getRequests().get("storage").toString());
  }

  @Test
  void generateResource_withDefaultCluster_shouldHaveVolumeClaimWithReadWriteOnceAccess() {
    List<HasMetadata> resources = clusterStatefulSet.generateResource(context).toList();

    StatefulSet sts = (StatefulSet) resources.getFirst();
    assertTrue(sts.getSpec().getVolumeClaimTemplates().getFirst()
        .getSpec().getAccessModes().contains("ReadWriteOnce"));
  }

  @Test
  void generateResource_withDefaultCluster_shouldHaveCorrectDataVolumeName() {
    String expectedDataName = StackGresUtil.statefulSetDataPersistentVolumeClaimName(cluster);

    List<HasMetadata> resources = clusterStatefulSet.generateResource(context).toList();

    StatefulSet sts = (StatefulSet) resources.getFirst();
    assertEquals(expectedDataName,
        sts.getSpec().getVolumeClaimTemplates().getFirst().getMetadata().getName());
  }

  @Test
  void generateResource_withDefaultCluster_shouldHavePersistentVolumeClaimRetentionPolicy() {
    List<HasMetadata> resources = clusterStatefulSet.generateResource(context).toList();

    StatefulSet sts = (StatefulSet) resources.getFirst();
    assertNotNull(sts.getSpec().getPersistentVolumeClaimRetentionPolicy());
    assertEquals("Delete",
        sts.getSpec().getPersistentVolumeClaimRetentionPolicy().getWhenDeleted());
    assertEquals("Retain",
        sts.getSpec().getPersistentVolumeClaimRetentionPolicy().getWhenScaled());
  }

  @Test
  void generateResource_withDefaultCluster_shouldHaveOrderedReadyPodManagementPolicy() {
    List<HasMetadata> resources = clusterStatefulSet.generateResource(context).toList();

    StatefulSet sts = (StatefulSet) resources.getFirst();
    assertEquals("OrderedReady", sts.getSpec().getPodManagementPolicy());
  }

  @Test
  void generateResource_withClaimedVolumes_shouldIncludeVolumeDependencies() {
    Volume volume = new VolumeBuilder().withName("extra-vol").build();
    HasMetadata volumeSource = new io.fabric8.kubernetes.api.model.ConfigMapBuilder()
        .withNewMetadata().withName("extra-cm").endMetadata().build();
    VolumePair volumePair = ImmutableVolumePair.builder()
        .volume(volume)
        .source(volumeSource)
        .build();
    when(volumeDiscoverer.discoverVolumes(any())).thenReturn(Map.of("extra-vol", volumePair));

    PodTemplateSpec podTemplateSpec = new PodTemplateSpecBuilder()
        .withNewMetadata().endMetadata()
        .withNewSpec().endSpec()
        .build();
    PodTemplateResult podTemplateResult = ImmutablePodTemplateResult.builder()
        .spec(podTemplateSpec)
        .claimedVolumes(List.of("extra-vol"))
        .build();
    when(podTemplateFactory.getPodTemplateSpec(any())).thenReturn(podTemplateResult);

    List<HasMetadata> resources = clusterStatefulSet.generateResource(context).toList();

    assertEquals(2, resources.size());
    assertTrue(resources.getFirst() instanceof StatefulSet);
    assertEquals("extra-cm", resources.get(1).getMetadata().getName());
  }

  @Test
  void generateResource_withDefaultCluster_shouldUseClusterNameAsServiceName() {
    List<HasMetadata> resources = clusterStatefulSet.generateResource(context).toList();

    StatefulSet sts = (StatefulSet) resources.getFirst();
    assertEquals(cluster.getMetadata().getName(), sts.getSpec().getServiceName());
  }

}

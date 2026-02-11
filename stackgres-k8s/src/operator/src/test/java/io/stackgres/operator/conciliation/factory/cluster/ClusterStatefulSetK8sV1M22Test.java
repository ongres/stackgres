/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ClusterLabelFactory;
import io.stackgres.common.labels.ClusterLabelMapper;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ImmutablePodTemplateResult;
import io.stackgres.operator.conciliation.factory.PodTemplateFactory;
import io.stackgres.operator.conciliation.factory.PodTemplateResult;
import io.stackgres.operator.conciliation.factory.VolumeDiscoverer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterStatefulSetK8sV1M22Test {

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

  private ClusterStatefulSetK8sV1M22 clusterStatefulSetK8sV1M22;

  private ClusterStatefulSet clusterStatefulSet;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    clusterStatefulSetK8sV1M22 = new ClusterStatefulSetK8sV1M22(
        labelFactory, podTemplateSpecFactoryDiscoverer, volumeDiscoverer);
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
  void generateResource_withK8sV1M22_shouldGenerateStatefulSet() {
    List<HasMetadata> resources = clusterStatefulSetK8sV1M22.generateResource(context).toList();

    assertFalse(resources.isEmpty());
    assertTrue(resources.getFirst() instanceof StatefulSet);
  }

  @Test
  void generateResource_withK8sV1M22_shouldNotHavePersistentVolumeClaimRetentionPolicy() {
    List<HasMetadata> resources = clusterStatefulSetK8sV1M22.generateResource(context).toList();

    StatefulSet sts = (StatefulSet) resources.getFirst();
    assertNull(sts.getSpec().getPersistentVolumeClaimRetentionPolicy());
  }

  @Test
  void generateResource_withParentClass_shouldHavePersistentVolumeClaimRetentionPolicy() {
    List<HasMetadata> resources = clusterStatefulSet.generateResource(context).toList();

    StatefulSet sts = (StatefulSet) resources.getFirst();
    assertNotNull(sts.getSpec().getPersistentVolumeClaimRetentionPolicy());
    assertEquals("Delete",
        sts.getSpec().getPersistentVolumeClaimRetentionPolicy().getWhenDeleted());
    assertEquals("Retain",
        sts.getSpec().getPersistentVolumeClaimRetentionPolicy().getWhenScaled());
  }

  @Test
  void generateResource_withK8sV1M22_shouldHaveCorrectNameAndNamespace() {
    List<HasMetadata> resources = clusterStatefulSetK8sV1M22.generateResource(context).toList();

    StatefulSet sts = (StatefulSet) resources.getFirst();
    assertEquals(cluster.getMetadata().getName(), sts.getMetadata().getName());
    assertEquals(cluster.getMetadata().getNamespace(), sts.getMetadata().getNamespace());
  }

  @Test
  void generateResource_withK8sV1M22_shouldHaveCorrectReplicaCount() {
    List<HasMetadata> resources = clusterStatefulSetK8sV1M22.generateResource(context).toList();

    StatefulSet sts = (StatefulSet) resources.getFirst();
    assertEquals(cluster.getSpec().getInstances(), sts.getSpec().getReplicas());
  }

  @Test
  void generateResource_withK8sV1M22_shouldHaveOnDeleteUpdateStrategy() {
    List<HasMetadata> resources = clusterStatefulSetK8sV1M22.generateResource(context).toList();

    StatefulSet sts = (StatefulSet) resources.getFirst();
    assertEquals("OnDelete", sts.getSpec().getUpdateStrategy().getType());
  }

  @Test
  void generateResource_withK8sV1M22_shouldHaveVolumeClaimTemplates() {
    List<HasMetadata> resources = clusterStatefulSetK8sV1M22.generateResource(context).toList();

    StatefulSet sts = (StatefulSet) resources.getFirst();
    assertNotNull(sts.getSpec().getVolumeClaimTemplates());
    assertFalse(sts.getSpec().getVolumeClaimTemplates().isEmpty());
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ClusterLabelMapper;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.InitContainerFactoryDiscoverer;
import io.stackgres.operator.conciliation.RunningContainerFactoryDiscoverer;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.ModelTestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PodTemplateSpecFactoryTest {

  private static final String POSTGRES_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions().findFirst().get();

  @Mock
  private ResourceFactory<StackGresClusterContext, PodSecurityContext> podSecurityContext;

  @Mock
  private LabelFactoryForCluster<StackGresCluster> labelFactory;

  @Mock
  private ClusterLabelMapper labelMapper;

  @Mock
  private RunningContainerFactoryDiscoverer<ClusterContainerContext> containerFactoryDiscoverer;

  @Mock
  private InitContainerFactoryDiscoverer<ClusterContainerContext>
      initContainerFactoryDiscoverer;

  @Mock
  private ContainerFactory<ClusterContainerContext> containerFactory;

  @Mock
  private Container patroniContainer;

  @InjectMocks
  private ClusterPodTemplateSpecFactory podTemplateSpecFactory;

  @Mock
  private ClusterContainerContext clusterContainerContext;

  @Mock
  private StackGresClusterContext clusterContext;

  private StackGresCluster cluster;

  @BeforeEach
  public void setupClass() {
    this.podTemplateSpecFactory = new ClusterPodTemplateSpecFactory(
        podSecurityContext, labelFactory, containerFactoryDiscoverer,
        initContainerFactoryDiscoverer);
    cluster = Fixtures.cluster().loadDefault().get();
    cluster.getSpec().getPostgres().setVersion(POSTGRES_VERSION);
  }

  @Test
  void clusterWithVolumes_shouldAddOnlyUsedVolumes() {
    when(clusterContainerContext.getClusterContext()).thenReturn(clusterContext);
    when(clusterContext.getSource()).thenReturn(cluster);
    when(clusterContext.getCluster()).thenReturn(cluster);
    when(labelFactory.labelMapper()).thenReturn(labelMapper);
    when(containerFactoryDiscoverer.discoverContainers(clusterContainerContext))
        .thenReturn(List.of(containerFactory));
    when(containerFactory.getContainer(clusterContainerContext))
        .thenReturn(patroniContainer);
    var requestedVolumeName = ModelTestUtil.createWithRandomData(String.class);
    var usedVolumeMount = new VolumeMountBuilder()
        .withName(requestedVolumeName)
        .build();
    when(patroniContainer.getVolumeMounts()).thenReturn(List.of(usedVolumeMount));

    Volume usedVolume = ModelTestUtil.createWithRandomData(Volume.class);
    Volume unusedVolume = ModelTestUtil.createWithRandomData(Volume.class);
    var availableVolumes = Map.of(
        requestedVolumeName,
        JsonUtil.copy(usedVolume),
        "unused-volume",
        unusedVolume);

    when(clusterContainerContext.availableVolumes()).thenReturn(availableVolumes);
    var podTemplateSpec = podTemplateSpecFactory.getPodTemplateSpec(clusterContainerContext);
    assertTrue(podTemplateSpec.getSpec().getSpec().getVolumes().size() == 1);
    assertTrue(podTemplateSpec.getSpec().getSpec().getVolumes().get(0).getName()
        .equals(usedVolume.getName()));
    assertTrue(podTemplateSpec.getSpec().getSpec().getVolumes().get(0).equals(usedVolume));
  }

  @Test
  void clusterWithVolumes_failUsingUndeclaredVolume() {
    VolumeMount undeclaredVolumeMount = new VolumeMountBuilder()
        .withName("undeclared-volume")
        .build();
    when(containerFactoryDiscoverer.discoverContainers(clusterContainerContext))
        .thenReturn(List.of(containerFactory));
    when(containerFactory.getContainer(clusterContainerContext))
        .thenReturn(patroniContainer);
    when(patroniContainer.getVolumeMounts()).thenReturn(List.of(undeclaredVolumeMount));

    Volume availableVolume = ModelTestUtil.createWithRandomData(Volume.class);;
    var availableVolumes = Map.of(
        "available-volume",
        JsonUtil.copy(availableVolume));
    when(clusterContainerContext.availableVolumes()).thenReturn(availableVolumes);
    when(clusterContainerContext.getDataVolumeName()).thenReturn("available-volume");
    assertThrows(IllegalStateException.class,
        () -> podTemplateSpecFactory.getPodTemplateSpec(clusterContainerContext));
  }
}

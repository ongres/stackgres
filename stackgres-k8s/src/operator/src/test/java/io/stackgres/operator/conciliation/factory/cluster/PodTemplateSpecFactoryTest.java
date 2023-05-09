/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import io.fabric8.kubernetes.api.model.*;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ClusterLabelMapper;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.ContainerFactoryDiscoverer;
import io.stackgres.operator.conciliation.InitContainerFactoryDiscover;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

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
  private ContainerFactoryDiscoverer<ClusterContainerContext> containerFactoryDiscoverer;

  @Mock
  private InitContainerFactoryDiscover<ClusterContainerContext>
      initContainerFactoryDiscoverer;

  @Mock
  private ContainerFactory<ClusterContainerContext> patroniContainerFactory;

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
        .thenReturn(List.of(patroniContainerFactory));
    when(patroniContainerFactory.getContainer(clusterContainerContext))
        .thenReturn(patroniContainer);
    when(patroniContainer.getVolumeMounts()).thenReturn(List.of(
        new VolumeMountBuilder()
            .withName("used-volume")
            .build()));

    Volume usedVolume = new VolumeBuilder()
        .withName("used-volume")
        .build();
    var availableVolumes = Map.of(
        "used-volume",
        JsonUtil.copy(usedVolume),
        "unused-volume",
        new VolumeBuilder()
            .withName("unused-volume")
            .build());

    when(clusterContainerContext.availableVolumes()).thenReturn(availableVolumes);
    var podTemplateSpec = podTemplateSpecFactory.getPodTemplateSpec(clusterContainerContext);
    assertTrue(podTemplateSpec.getSpec().getSpec().getVolumes().size() == 1);
    assertTrue(
        podTemplateSpec.getSpec().getSpec().getVolumes().get(0).getName().equals("used-volume"));
    assertTrue(podTemplateSpec.getSpec().getSpec().getVolumes().get(0).equals(usedVolume));
  }
}

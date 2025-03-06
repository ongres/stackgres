/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterPodsContextAppenderTest {

  private ClusterPodsContextAppender contextAppender;

  private StackGresCluster cluster;

  @Spy
  private StackGresClusterContext.Builder contextBuilder;

  @Mock
  private LabelFactoryForCluster labelFactory;

  @Mock
  private ResourceScanner<Pod> podScanner;

  @Mock
  private ClusterPodDataPersistentVolumeNamesContextAppender clusterPodDataPersistentVolumeNamesContextAppender;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    contextAppender = new ClusterPodsContextAppender(
        labelFactory, podScanner, clusterPodDataPersistentVolumeNamesContextAppender);
  }

  @Test
  void givenClusterWithoutPods_shouldPass() {
    contextAppender.appendContext(cluster, contextBuilder);

    verify(podScanner).getResourcesInNamespaceWithLabels(any(), any());
    verify(contextBuilder).currentInstances(0);
  }

  @Test
  void givenClusterWithPods_shouldPass() {
    final List<Pod> pods = List.of(
        new PodBuilder()
        .withNewMetadata()
        .withName(cluster.getMetadata().getName() + "-0")
        .endMetadata()
        .build(),
        new PodBuilder()
        .withNewMetadata()
        .withName(cluster.getMetadata().getName() + "-1")
        .endMetadata()
        .build());
    when(podScanner.getResourcesInNamespaceWithLabels(any(), any()))
        .thenReturn(pods);
    contextAppender.appendContext(cluster, contextBuilder);

    verify(podScanner).getResourcesInNamespaceWithLabels(any(), any());
    verify(contextBuilder).currentInstances(pods.size());
  }

}

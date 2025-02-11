/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
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
class ClusterPodDataPersistentVolumeNamesContextAppenderTest {

  private ClusterPodDataPersistentVolumeNamesContextAppender contextAppender;

  private StackGresCluster cluster;

  @Spy
  private StackGresClusterContext.Builder contextBuilder;

  @Mock
  private LabelFactoryForCluster labelFactory;

  @Mock
  private ResourceScanner<PersistentVolumeClaim> pvcScanner;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    contextAppender = new ClusterPodDataPersistentVolumeNamesContextAppender(
        labelFactory, pvcScanner);
  }

  @Test
  void givenClusterWithoutPvcs_shouldPass() {
    contextAppender.appendContext(cluster, List.of(), contextBuilder);

    verify(pvcScanner).getResourcesInNamespaceWithLabels(any(), any());
    verify(contextBuilder).podDataPersistentVolumeNames(Map.of());
  }

  @Test
  void givenClusterWithPvcs_shouldPass() {
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
    final List<PersistentVolumeClaim> pvcs = List.of(
        new PersistentVolumeClaimBuilder()
        .withNewMetadata()
        .withName(cluster.getMetadata().getName() + "-data-" + cluster.getMetadata().getName() + "-0")
        .endMetadata()
        .withNewSpec()
        .withVolumeName("pv-0")
        .endSpec()
        .build(),
        new PersistentVolumeClaimBuilder()
        .withNewMetadata()
        .withName(cluster.getMetadata().getName() + "-data-" + cluster.getMetadata().getName() + "-1")
        .endMetadata()
        .withNewSpec()
        .withVolumeName("pv-1")
        .endSpec()
        .build());
    when(pvcScanner.getResourcesInNamespaceWithLabels(any(), any()))
        .thenReturn(pvcs);
    contextAppender.appendContext(cluster, pods, contextBuilder);

    verify(pvcScanner).getResourcesInNamespaceWithLabels(any(), any());
    verify(contextBuilder).podDataPersistentVolumeNames(
        Map.of(
            cluster.getMetadata().getName() + "-0", "pv-0",
            cluster.getMetadata().getName() + "-1", "pv-1"));
  }

}

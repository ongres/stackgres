/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config.context;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBuilder;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ClusterLabelFactory;
import io.stackgres.common.labels.ClusterLabelMapper;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operator.common.ObservedClusterContext;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigObservedClustersContextAppenderTest {

  private ConfigObservedClustersContextAppender contextAppender;

  private StackGresConfig config;

  @Spy
  private StackGresConfigContext.Builder contextBuilder;

  @Mock
  private CustomResourceScanner<StackGresCluster> clusterScanner;

  private LabelFactoryForCluster labelFactoryForCluster;

  @Mock
  private ResourceScanner<Pod> podScanner;

  @BeforeEach
  void setUp() {
    config = Fixtures.config().loadDefault().get();
    labelFactoryForCluster = new ClusterLabelFactory(new ClusterLabelMapper());
    contextAppender = new ConfigObservedClustersContextAppender(
        clusterScanner, labelFactoryForCluster, podScanner);
  }

  @Test
  void givenConfigWithoutClusters_shouldPass() {
    contextAppender.appendContext(config, contextBuilder);

    verify(contextBuilder).observedClusters(List.of());
    verify(clusterScanner).getResources();
    verify(podScanner, Mockito.never()).getResourcesInNamespaceWithLabels(Mockito.any(), Mockito.any());
  }

  @Test
  void givenConfigWitClustersNotConfigured_shouldPass() {
    StackGresCluster cluster =
        new StackGresClusterBuilder()
        .withNewMetadata()
        .withUid("uid")
        .withName("not-observed")
        .endMetadata()
        .build();
    when(clusterScanner.getResources())
        .thenReturn(List.of(cluster));
    contextAppender.appendContext(config, contextBuilder);

    verify(contextBuilder).observedClusters(List.of());
    verify(podScanner, Mockito.never()).getResourcesInNamespaceWithLabels(Mockito.any(), Mockito.any());
  }

  @Test
  void givenConfigWitClustersNotObserved_shouldPass() {
    StackGresCluster cluster =
        new StackGresClusterBuilder()
        .withNewMetadata()
        .withUid("uid")
        .withName("not-observed")
        .endMetadata()
        .withNewSpec()
        .withNewConfigurations()
        .withNewObservability()
        .withPrometheusAutobind(false)
        .endObservability()
        .endConfigurations()
        .endSpec()
        .build();
    when(clusterScanner.getResources())
        .thenReturn(List.of(cluster));
    contextAppender.appendContext(config, contextBuilder);

    verify(contextBuilder).observedClusters(List.of());
    verify(podScanner, Mockito.never()).getResourcesInNamespaceWithLabels(Mockito.any(), Mockito.any());
  }

  @Test
  void givenConfigWitClustersObserved_shouldPass() {
    StackGresCluster cluster =
        new StackGresClusterBuilder()
        .withNewMetadata()
        .withUid("uid")
        .withName("observed")
        .endMetadata()
        .withNewSpec()
        .withNewConfigurations()
        .withNewObservability()
        .withPrometheusAutobind(true)
        .endObservability()
        .endConfigurations()
        .endSpec()
        .build();
    when(clusterScanner.getResources())
        .thenReturn(List.of(cluster));
    contextAppender.appendContext(config, contextBuilder);

    verify(contextBuilder).observedClusters(
        List.of(ObservedClusterContext.toObservedClusterContext(
            cluster,
            List.of())));
    verify(podScanner).getResourcesInNamespaceWithLabels(
        cluster.getMetadata().getNamespace(),
        labelFactoryForCluster.clusterLabels(cluster));
  }

  @Test
  void givenConfigWitClustersObservedWithAPod_shouldPass() {
    StackGresCluster cluster =
        new StackGresClusterBuilder()
        .withNewMetadata()
        .withUid("uid")
        .withName("observed")
        .endMetadata()
        .withNewSpec()
        .withNewConfigurations()
        .withNewObservability()
        .withPrometheusAutobind(true)
        .endObservability()
        .endConfigurations()
        .endSpec()
        .build();
    Pod pod =
        new PodBuilder()
        .withNewMetadata()
        .withUid("uid")
        .withNamespace("namespace")
        .withName("observed-0")
        .withCreationTimestamp("2025-02-14T13:00:00Z")
        .endMetadata()
        .withNewStatus()
        .withPodIP("1.2.3.4")
        .endStatus()
        .build();
    when(clusterScanner.getResources())
        .thenReturn(List.of(cluster));
    when(podScanner.getResourcesInNamespaceWithLabels(
        cluster.getMetadata().getNamespace(),
        labelFactoryForCluster.clusterLabels(cluster)))
        .thenReturn(List.of(pod));
    contextAppender.appendContext(config, contextBuilder);

    verify(contextBuilder).observedClusters(
        List.of(ObservedClusterContext.toObservedClusterContext(
            cluster,
            List.of(pod))));
  }

}

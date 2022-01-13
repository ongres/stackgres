/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static io.stackgres.operator.fixture.NodeAffinityFixture.PREFERRED_TOPOLOGY_KEY;
import static io.stackgres.operator.fixture.NodeAffinityFixture.REQUIRED_TOPOLY_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.crd.NodeAffinity;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conciliation.ContainerFactoryDiscoverer;
import io.stackgres.operator.conciliation.InitContainerFactoryDiscover;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.fixture.NodeAffinityFixture;
import io.stackgres.operator.fixture.StackGresClusterFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class PodTemplateSpecFactoryTest {

  @Mock
  private ResourceFactory<StackGresClusterContext, PodSecurityContext> podSecurityContext;

  @Mock
  private LabelFactoryForCluster<StackGresCluster> labelFactory;

  @Mock
  private ContainerFactoryDiscoverer<StackGresClusterContainerContext> containerFactoryDiscoverer;

  @Mock
  private InitContainerFactoryDiscover<StackGresClusterContainerContext>
      initContainerFactoryDiscoverer;

  @InjectMocks
  private PodTemplateSpecFactory podTemplateSpecFactory;

  @BeforeEach
  public void setupClass() {
    this.podTemplateSpecFactory = new PodTemplateSpecFactory(
        podSecurityContext, labelFactory, containerFactoryDiscoverer,
        initContainerFactoryDiscoverer);
  }

  @Test
  void shouldNodeAffinity_HasTheSameRequireDuringSchedulingRequirementsSize() {
    NodeAffinity nodeAffinity = new NodeAffinityFixture()
        .withValidRequirement()
        .build();
    StackGresCluster cluster = new StackGresClusterFixture().withNodeAffinity(nodeAffinity).build();
    io.fabric8.kubernetes.api.model.NodeAffinity k8sPodNodeAffinity =
        cluster.getSpec().getPod().getScheduling().getNodeAffinity();
    assertEquals(2,
        k8sPodNodeAffinity
            .getRequiredDuringSchedulingIgnoredDuringExecution()
            .getNodeSelectorTerms().size());
  }

  @Test
  void shouldNodeAffinity_HasTheSameRequireDuringSchedulingRequirementsKey() {
    NodeAffinity nodeAffinity = new NodeAffinityFixture()
        .withValidRequirement()
        .build();
    StackGresCluster cluster = new StackGresClusterFixture()
        .withNodeAffinity(nodeAffinity)
        .build();
    io.fabric8.kubernetes.api.model.NodeAffinity k8sPodNodeAffinity =
        cluster.getSpec().getPod().getScheduling().getNodeAffinity();
    k8sPodNodeAffinity.getRequiredDuringSchedulingIgnoredDuringExecution().getNodeSelectorTerms()
        .forEach(term -> {
          term.getMatchExpressions().forEach(math -> {
            assertEquals(REQUIRED_TOPOLY_KEY, math.getKey());
          });
        });
  }

  @Test
  void shouldNodeAffinity_HasTheSamePreferredDuringSchedulingRequirementsSize() {
    NodeAffinity nodeAffinity = new NodeAffinityFixture()
        .withValidPreferredScheduling()
        .build();
    StackGresCluster cluster = new StackGresClusterFixture().withNodeAffinity(nodeAffinity).build();
    io.fabric8.kubernetes.api.model.NodeAffinity k8sPodNodeAffinity =
        cluster.getSpec().getPod().getScheduling().getNodeAffinity();
    assertEquals(nodeAffinity.getPreferredDuringSchedulingIgnoredDuringExecution().size(),
        k8sPodNodeAffinity.getPreferredDuringSchedulingIgnoredDuringExecution().size());
  }

  @Test
  void shouldNodeAffinity_HasTheSamePreferredDuringSchedulingRequirementsKey() {
    NodeAffinity nodeAffinity = new NodeAffinityFixture()
        .withValidPreferredScheduling()
        .build();
    StackGresCluster cluster = new StackGresClusterFixture().withNodeAffinity(nodeAffinity).build();
    io.fabric8.kubernetes.api.model.NodeAffinity k8sPodNodeAffinity =
        cluster.getSpec().getPod().getScheduling().getNodeAffinity();
    k8sPodNodeAffinity.getPreferredDuringSchedulingIgnoredDuringExecution().forEach(preference -> {
      preference.getPreference().getMatchExpressions().forEach(math -> {
        assertEquals(PREFERRED_TOPOLOGY_KEY, math.getKey());
      });
    });
  }
}

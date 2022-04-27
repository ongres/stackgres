/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

import static io.stackgres.common.fixture.NodeAffinityFixture.PREFERRED_TOPOLOGY_KEY;
import static io.stackgres.common.fixture.NodeAffinityFixture.REQUIRED_TOPOLY_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.NodeAffinityFixture;
import io.stackgres.common.fixture.StackGresClusterFixture;
import org.junit.jupiter.api.Test;

class NodeAffinityTest {

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

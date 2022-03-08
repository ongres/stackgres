/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import io.fabric8.kubernetes.api.model.NodeSelector;
import io.fabric8.kubernetes.api.model.PreferredSchedulingTerm;
import io.stackgres.common.fixture.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StackGresClusterPodSchedulingTest {

  private static final int TWO_NODE_AFFFINITY_EXPRESSION_VALUES = 2;
  private static final String OPERATOR_IN = "In";
  private static final String REQUIRED_TOPOLOGY_KEY = "kubernetes.io/e2e-az-name-required";
  private static final String PREFERRED_TOPOLOGY_KEY = "kubernetes.io/e2e-az-name-preferred";
  private NodeSelector nodeAffinityRequiredDuringScheduling;
  private List<PreferredSchedulingTerm> nodeAffinityPreferredDuringScheduling;

  @BeforeEach
  public void setup() {
    StackGresClusterPodScheduling podNodeAffinityScheduling = Fixtures.cluster().scheduling()
        .loadDefault().get();
    this.nodeAffinityRequiredDuringScheduling = podNodeAffinityScheduling
        .getNodeAffinity()
        .getRequiredDuringSchedulingIgnoredDuringExecution();
    this.nodeAffinityPreferredDuringScheduling = podNodeAffinityScheduling
        .getNodeAffinity()
        .getPreferredDuringSchedulingIgnoredDuringExecution();
  }

  @Test
  void shouldMatchExpressionKey_BeEqualsForPodNodeAffinityRequiredDuringScheduling() {
    nodeAffinityRequiredDuringScheduling.getNodeSelectorTerms().get(0)
        .getMatchExpressions().forEach(matchExpression -> {
          assertTrue(matchExpression.getKey().equals(REQUIRED_TOPOLOGY_KEY));
        });
  }

  @Test
  void shouldOperator_BeEqualsInForPodNodeAffinityRequiredDuringScheduling() {
    nodeAffinityRequiredDuringScheduling.getNodeSelectorTerms().get(0)
        .getMatchExpressions().forEach(matchExpression -> {
          assertTrue(matchExpression.getOperator().equals(OPERATOR_IN));
        });
  }

  @Test
  void shouldMathExpressionValuesSize_BeEqualsPodNodeAffinityRequiredDuringSchedulingSize() {
    assertEquals(TWO_NODE_AFFFINITY_EXPRESSION_VALUES,
        nodeAffinityRequiredDuringScheduling.getNodeSelectorTerms().get(0)
            .getMatchExpressions().get(0).getValues().size());
  }

  @Test
  void shouldMatchExpressionKey_BeEqualsForPodNodeAffinityPreferredDuringScheduling() {
    nodeAffinityPreferredDuringScheduling.get(0).getPreference()
        .getMatchExpressions().forEach(matchExpression -> {
          assertTrue(matchExpression.getKey().equals(PREFERRED_TOPOLOGY_KEY));
        });
  }

  @Test
  void shouldOperator_BeEqualsInForPodNodeAffinityPreferredDuringScheduling() {
    nodeAffinityPreferredDuringScheduling.get(0)
        .getPreference().getMatchExpressions().forEach(matchExpression -> {
          assertEquals(matchExpression.getOperator(), OPERATOR_IN);
        });
  }

  @Test
  void shouldMathExpressionValuesSize_BeEqualsPodNodeAffinityPreferredDuringSchedulingSize() {
    assertEquals(TWO_NODE_AFFFINITY_EXPRESSION_VALUES,
        nodeAffinityPreferredDuringScheduling.get(0).getPreference()
            .getMatchExpressions().get(0).getValues().size());
  }
}

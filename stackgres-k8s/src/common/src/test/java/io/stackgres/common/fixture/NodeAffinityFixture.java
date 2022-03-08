/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import java.util.ArrayList;
import java.util.List;

import io.fabric8.kubernetes.api.model.NodeAffinityBuilder;
import io.fabric8.kubernetes.api.model.NodeSelector;
import io.fabric8.kubernetes.api.model.NodeSelectorBuilder;
import io.fabric8.kubernetes.api.model.PreferredSchedulingTerm;
import io.stackgres.common.crd.NodeAffinity;

public class NodeAffinityFixture {

  public static final String REQUIRED_TOPOLY_KEY = "kubernetes.io/e2e-az-name-required";
  public static final String PREFERRED_TOPOLOGY_KEY = "kubernetes.io/e2e-az-preferred";
  private NodeSelector requireds = new NodeSelectorBuilder().build();
  private List<PreferredSchedulingTerm> preference = new ArrayList<PreferredSchedulingTerm>();

  public NodeAffinityFixture withRequireds(NodeSelector requireds) {
    this.requireds = requireds;
    return this;
  }

  public NodeAffinityFixture withPreferredbuild(List<PreferredSchedulingTerm> preference) {
    this.preference = preference;
    return this;
  }

  public NodeAffinityFixture withValidRequirement() {
    requireds = new NodeSelectorFixture()
        .withTerms(new NodeSelectorTermFixture()
            .withRequirement(new NodeSelectorRequirementFixture()
                .withKey(REQUIRED_TOPOLY_KEY)
                .withOperator("In")
                .withValue("e2e-az1", "e2e-az2")
                .build())
            .build(),
            new NodeSelectorTermFixture()
                .withRequirement(new NodeSelectorRequirementFixture()
                    .withKey(REQUIRED_TOPOLY_KEY)
                    .withOperator("In")
                    .withValue("e2e-az3", "e2e-az4")
                    .build())
                .build())
        .buildAsRequirements();
    return this;
  }

  public NodeAffinity build() {
    NodeAffinity nodeAffinity = new NodeAffinity();
    io.fabric8.kubernetes.api.model.NodeAffinity k8sNodeAffinity = new NodeAffinityBuilder()
        .withRequiredDuringSchedulingIgnoredDuringExecution(requireds)
        .withPreferredDuringSchedulingIgnoredDuringExecution(preference)
        .build();
    nodeAffinity.setPreferredDuringSchedulingIgnoredDuringExecution(
        k8sNodeAffinity.getPreferredDuringSchedulingIgnoredDuringExecution());
    nodeAffinity.setRequiredDuringSchedulingIgnoredDuringExecution(
        k8sNodeAffinity.getRequiredDuringSchedulingIgnoredDuringExecution());
    return nodeAffinity;
  }

  public NodeAffinityFixture withValidPreferredScheduling() {
    preference.add(new PreferredSchedulingTermFixture()
        .withWeight(1).withRequirement(new NodeSelectorRequirementFixture()
            .withKey(PREFERRED_TOPOLOGY_KEY)
            .withOperator("In")
            .withValue("e2e-az1", "e2e-az2")
            .build())
        .build());

    preference.add(new PreferredSchedulingTermFixture()
        .withWeight(1).withRequirement(new NodeSelectorRequirementFixture()
            .withKey(PREFERRED_TOPOLOGY_KEY)
            .withOperator("In")
            .withValue("e2e-az3", "e2e-az4")
            .build())
        .build());
    return this;
  }
}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import java.util.Arrays;

import io.fabric8.kubernetes.api.model.NodeSelectorRequirement;
import io.fabric8.kubernetes.api.model.PreferredSchedulingTerm;
import io.fabric8.kubernetes.api.model.PreferredSchedulingTermBuilder;

public class PreferredSchedulingTermFixture {

  private int weight;
  private NodeSelectorRequirement nodeSelectorRequirement;

  public PreferredSchedulingTermFixture withWeight(int weight) {
    this.weight = weight;
    return this;
  }

  public PreferredSchedulingTermFixture withRequirement(NodeSelectorRequirement
      nodeSelectorRequirement) {
    this.nodeSelectorRequirement = nodeSelectorRequirement;
    return this;
  }

  public PreferredSchedulingTerm build() {
    return new PreferredSchedulingTermBuilder()
      .withWeight(this.weight)
      .withNewPreference()
        .addAllToMatchExpressions(Arrays.asList(this.nodeSelectorRequirement))
        .endPreference()
     .build();
  }

  public PreferredSchedulingTermBuilder getBuilder() {
    return new PreferredSchedulingTermBuilder(build());
  }

}

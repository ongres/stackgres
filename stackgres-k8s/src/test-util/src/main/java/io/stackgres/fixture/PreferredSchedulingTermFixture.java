/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.fixture;

import java.util.Arrays;

import io.fabric8.kubernetes.api.model.NodeSelectorRequirement;
import io.fabric8.kubernetes.api.model.PreferredSchedulingTerm;
import io.fabric8.kubernetes.api.model.PreferredSchedulingTermBuilder;

public class PreferredSchedulingTermFixture {

  private int weigth;
  private NodeSelectorRequirement nodeSelectorRequirement;

  public PreferredSchedulingTermFixture withWeight(int weigth) {
    this.weigth = weigth;
    return this;
  }

  public PreferredSchedulingTermFixture withRequirement(NodeSelectorRequirement
      nodeSelectorRequirement) {
    this.nodeSelectorRequirement = nodeSelectorRequirement;
    return this;
  }

  public PreferredSchedulingTerm build() {
    return new PreferredSchedulingTermBuilder()
      .withWeight(this.weigth)
      .withNewPreference()
        .addAllToMatchExpressions(Arrays.asList(this.nodeSelectorRequirement))
        .endPreference()
     .build();
  }
}

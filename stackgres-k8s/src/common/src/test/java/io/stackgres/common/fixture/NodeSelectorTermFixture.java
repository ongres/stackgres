/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import java.util.Arrays;

import io.fabric8.kubernetes.api.model.NodeSelectorRequirement;
import io.fabric8.kubernetes.api.model.NodeSelectorTerm;
import io.fabric8.kubernetes.api.model.NodeSelectorTermBuilder;

public class NodeSelectorTermFixture {

  private NodeSelectorRequirement requirement;

  public NodeSelectorTermFixture withRequirement(NodeSelectorRequirement requirement) {
    this.requirement = requirement;
    return this;
  }

  public NodeSelectorTerm build() {
    return new NodeSelectorTermBuilder()
      .addAllToMatchExpressions(Arrays.asList(requirement))
      .build();
  }
}

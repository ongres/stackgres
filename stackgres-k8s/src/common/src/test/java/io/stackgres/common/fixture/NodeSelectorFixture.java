/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import io.fabric8.kubernetes.api.model.NodeAffinityBuilder;
import io.fabric8.kubernetes.api.model.NodeSelector;
import io.fabric8.kubernetes.api.model.NodeSelectorBuilder;
import io.fabric8.kubernetes.api.model.NodeSelectorTerm;

public class NodeSelectorFixture {

  private NodeSelectorTerm[] nodeSelectorTerms;

  public NodeSelectorFixture withTerms(NodeSelectorTerm...nodeSelectorTerms) {
    this.nodeSelectorTerms = nodeSelectorTerms;
    return this;
  }

  public NodeSelector buildAsRequirements() {
    return new NodeAffinityBuilder()
      .withNewRequiredDuringSchedulingIgnoredDuringExecution()
      .withNodeSelectorTerms(nodeSelectorTerms)
      .endRequiredDuringSchedulingIgnoredDuringExecution()
      .buildRequiredDuringSchedulingIgnoredDuringExecution();
  }

  public NodeSelectorBuilder getBuilder() {
    return new NodeSelectorBuilder(buildAsRequirements());
  }

}

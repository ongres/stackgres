/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import io.fabric8.kubernetes.api.model.NodeSelectorRequirement;
import io.fabric8.kubernetes.api.model.NodeSelectorRequirementBuilder;

public class NodeSelectorRequirementFixture {
  private String requirementKey;
  private String[] requirementValues;
  private String requirementOperator;

  public NodeSelectorRequirementFixture withKey(String key) {
    this.requirementKey = key;
    return this;
  }

  public NodeSelectorRequirementFixture withValue(String...values) {
    this.requirementValues = values;
    return this;
  }

  public NodeSelectorRequirementFixture withOperator(String operator) {
    this.requirementOperator = operator;
    return this;
  }

  public NodeSelectorRequirement build() {
    return new NodeSelectorRequirementBuilder()
      .withKey(requirementKey)
      .withOperator(requirementOperator)
      .withValues(requirementValues)
      .build();
  }
}

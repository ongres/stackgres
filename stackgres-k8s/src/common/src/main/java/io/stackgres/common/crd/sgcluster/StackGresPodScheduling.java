/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.validation.constraints.AssertTrue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.Toleration;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresPodScheduling {

  @JsonProperty("nodeSelector")
  private Map<String, String> nodeSelector;

  @JsonProperty("tolerations")
  private List<Toleration> tolerations;

  @ReferencedField("nodeSelector")
  interface NodeSelector extends FieldReference { }

  @ReferencedField("tolerations")
  interface TolerationField extends FieldReference { }

  @JsonIgnore
  @AssertTrue(message = "nodeSelector can not be empty.",
      payload = NodeSelector.class)
  public boolean isNodeSelectorNotEmpty() {
    return nodeSelector == null || !nodeSelector.isEmpty();
  }

  @JsonIgnore
  @AssertTrue(message = "toleration operator must be Exists when key is empty.",
      payload = TolerationField.class)
  public boolean isTolerationOperatorExistsWhenKeyIsEmpty() {
    return tolerations == null || tolerations.stream()
        .allMatch(toleration -> (toleration.getKey() != null && !toleration.getKey().isEmpty())
            || Objects.equals(toleration.getOperator(), "Exists"));
  }

  @JsonIgnore
  @AssertTrue(message = "toleration operator must be Equal or Exists.",
      payload = TolerationField.class)
  public boolean isTolerationOperatorValid() {
    return tolerations == null || tolerations.stream()
        .allMatch(toleration -> toleration.getOperator() == null
        || ImmutableList.of("Equal", "Exists").contains(toleration.getOperator()));
  }

  @JsonIgnore
  @AssertTrue(message = "toleration effect must be NoSchedule, PreferNoSchedule or NoExecute.",
      payload = TolerationField.class)
  public boolean isTolerationEffectValid() {
    return tolerations == null || tolerations.stream()
        .allMatch(toleration -> toleration.getEffect() == null
        || ImmutableList.of("NoSchedule", "PreferNoSchedule", "NoExecute")
        .contains(toleration.getEffect()));
  }

  public Map<String, String> getNodeSelector() {
    return nodeSelector;
  }

  public void setNodeSelector(Map<String, String> nodeSelector) {
    this.nodeSelector = nodeSelector;
  }

  public List<Toleration> getTolerations() {
    return tolerations;
  }

  public void setTolerations(List<Toleration> tolerations) {
    this.tolerations = tolerations;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresPodScheduling)) {
      return false;
    }
    StackGresPodScheduling other = (StackGresPodScheduling) obj;
    return Objects.equals(nodeSelector, other.nodeSelector)
        && Objects.equals(tolerations, other.tolerations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nodeSelector, tolerations);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("nodeSelector", nodeSelector)
        .add("tolerations", tolerations)
        .toString();
  }
}

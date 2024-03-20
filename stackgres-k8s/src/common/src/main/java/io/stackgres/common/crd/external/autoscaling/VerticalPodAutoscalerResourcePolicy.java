/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.external.autoscaling;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class VerticalPodAutoscalerResourcePolicy {

  private List<VerticalPodAutoscalerContainerPolicy> containerPolicies;

  public List<VerticalPodAutoscalerContainerPolicy> getContainerPolicies() {
    return containerPolicies;
  }

  public void setContainerPolicies(List<VerticalPodAutoscalerContainerPolicy> containerPolicies) {
    this.containerPolicies = containerPolicies;
  }

  @Override
  public int hashCode() {
    return Objects.hash(containerPolicies);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof VerticalPodAutoscalerResourcePolicy)) {
      return false;
    }
    VerticalPodAutoscalerResourcePolicy other = (VerticalPodAutoscalerResourcePolicy) obj;
    return Objects.equals(containerPolicies, other.containerPolicies);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

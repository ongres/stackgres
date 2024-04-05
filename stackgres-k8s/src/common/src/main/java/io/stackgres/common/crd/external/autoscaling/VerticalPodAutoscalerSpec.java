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
public class VerticalPodAutoscalerSpec {

  private List<VerticalPodAutoscalerRecommender> recommenders;

  private VerticalPodAutoscalerTargetRef targetRef;

  private VerticalPodAutoscalerUpdatePolicy updatePolicy;

  private VerticalPodAutoscalerResourcePolicy resourcePolicy;

  public List<VerticalPodAutoscalerRecommender> getRecommenders() {
    return recommenders;
  }

  public void setRecommenders(List<VerticalPodAutoscalerRecommender> recommenders) {
    this.recommenders = recommenders;
  }

  public VerticalPodAutoscalerTargetRef getTargetRef() {
    return targetRef;
  }

  public void setTargetRef(VerticalPodAutoscalerTargetRef targetRef) {
    this.targetRef = targetRef;
  }

  public VerticalPodAutoscalerUpdatePolicy getUpdatePolicy() {
    return updatePolicy;
  }

  public void setUpdatePolicy(VerticalPodAutoscalerUpdatePolicy updatePolicy) {
    this.updatePolicy = updatePolicy;
  }

  public VerticalPodAutoscalerResourcePolicy getResourcePolicy() {
    return resourcePolicy;
  }

  public void setResourcePolicy(VerticalPodAutoscalerResourcePolicy resourcePolicy) {
    this.resourcePolicy = resourcePolicy;
  }

  @Override
  public int hashCode() {
    return Objects.hash(recommenders, resourcePolicy, targetRef, updatePolicy);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof VerticalPodAutoscalerSpec)) {
      return false;
    }
    VerticalPodAutoscalerSpec other = (VerticalPodAutoscalerSpec) obj;
    return Objects.equals(recommenders, other.recommenders) && Objects.equals(resourcePolicy, other.resourcePolicy)
        && Objects.equals(targetRef, other.targetRef) && Objects.equals(updatePolicy, other.updatePolicy);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

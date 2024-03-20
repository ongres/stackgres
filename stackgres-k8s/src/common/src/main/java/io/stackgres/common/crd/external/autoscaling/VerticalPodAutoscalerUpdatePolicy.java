/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.external.autoscaling;

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
public class VerticalPodAutoscalerUpdatePolicy {

  private Integer minReplicas;

  private String updateMode;

  public Integer getMinReplicas() {
    return minReplicas;
  }

  public void setMinReplicas(Integer minReplicas) {
    this.minReplicas = minReplicas;
  }

  public String getUpdateMode() {
    return updateMode;
  }

  public void setUpdateMode(String updateMode) {
    this.updateMode = updateMode;
  }

  @Override
  public int hashCode() {
    return Objects.hash(minReplicas, updateMode);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof VerticalPodAutoscalerUpdatePolicy)) {
      return false;
    }
    VerticalPodAutoscalerUpdatePolicy other = (VerticalPodAutoscalerUpdatePolicy) obj;
    return Objects.equals(minReplicas, other.minReplicas) && Objects.equals(updateMode, other.updateMode);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

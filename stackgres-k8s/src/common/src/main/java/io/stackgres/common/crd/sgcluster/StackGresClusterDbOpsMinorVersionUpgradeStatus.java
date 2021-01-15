/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.List;
import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresClusterDbOpsMinorVersionUpgradeStatus implements KubernetesResource {

  private static final long serialVersionUID = -1;

  @JsonProperty("initialInstances")
  @NotNull
  private List<String> initialInstances;

  @JsonProperty("primaryInstance")
  @NotNull
  private String primaryInstance;

  public List<String> getInitialInstances() {
    return initialInstances;
  }

  public void setInitialInstances(List<String> initialInstances) {
    this.initialInstances = initialInstances;
  }

  public String getPrimaryInstance() {
    return primaryInstance;
  }

  public void setPrimaryInstance(String primaryInstance) {
    this.primaryInstance = primaryInstance;
  }

  @Override
  public int hashCode() {
    return Objects.hash(initialInstances, primaryInstance);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterDbOpsMinorVersionUpgradeStatus)) {
      return false;
    }
    StackGresClusterDbOpsMinorVersionUpgradeStatus other =
        (StackGresClusterDbOpsMinorVersionUpgradeStatus) obj;
    return Objects.equals(initialInstances, other.initialInstances)
        && Objects.equals(primaryInstance, other.primaryInstance);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

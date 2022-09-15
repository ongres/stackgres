/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ClusterSpecLabels {

  private Map<String, String> clusterPods;

  public Map<String, String> getClusterPods() {
    return clusterPods;
  }

  public void setClusterPods(Map<String, String> clusterPods) {
    this.clusterPods = clusterPods;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClusterSpecLabels that = (ClusterSpecLabels) o;
    return Objects.equals(clusterPods, that.clusterPods);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clusterPods);
  }
}

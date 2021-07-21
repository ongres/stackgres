/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresDbOpsPgbenchStatusLatency implements KubernetesResource {

  private static final long serialVersionUID = 1L;

  @JsonProperty("average")
  private StackGresDbOpsPgbenchStatusMeasure average;

  @JsonProperty("standardDeviation")
  private StackGresDbOpsPgbenchStatusMeasure standardDeviation;

  @Override
  public int hashCode() {
    return Objects.hash(average, standardDeviation);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDbOpsPgbenchStatusLatency)) {
      return false;
    }
    StackGresDbOpsPgbenchStatusLatency other = (StackGresDbOpsPgbenchStatusLatency) obj;
    return Objects.equals(average, other.average)
        && Objects.equals(standardDeviation, other.standardDeviation);
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgpgconfig;

import java.util.List;
import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresPostgresConfigStatus implements KubernetesResource {

  private static final long serialVersionUID = -5276087851826599719L;

  @JsonProperty("defaultParameters")
  @NotNull(message = "defaultParameters is required")
  private List<String> defaultParameters;

  public List<String> getDefaultParameters() {
    return defaultParameters;
  }

  public void setDefaultParameters(List<String> defaultParameters) {
    this.defaultParameters = defaultParameters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StackGresPostgresConfigStatus that = (StackGresPostgresConfigStatus) o;
    return Objects.equals(defaultParameters, that.defaultParameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(defaultParameters);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("defaultParameters", defaultParameters)
        .toString();
  }
}

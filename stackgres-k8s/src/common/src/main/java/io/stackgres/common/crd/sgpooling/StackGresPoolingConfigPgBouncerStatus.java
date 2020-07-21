/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgpooling;

import java.util.List;
import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresPoolingConfigPgBouncerStatus {

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
  public int hashCode() {
    return Objects.hash(defaultParameters);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresPoolingConfigPgBouncerStatus)) {
      return false;
    }
    StackGresPoolingConfigPgBouncerStatus other = (StackGresPoolingConfigPgBouncerStatus) obj;
    return Objects.equals(defaultParameters, other.defaultParameters);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("defaultParameters", defaultParameters)
        .toString();
  }
}

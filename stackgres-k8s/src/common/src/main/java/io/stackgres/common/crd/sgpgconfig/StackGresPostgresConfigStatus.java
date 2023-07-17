/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgpgconfig;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresPostgresConfigStatus {

  @JsonProperty("defaultParameters")
  @NotNull(message = "defaultParameters is required")
  private Map<String, String> defaultParameters;

  public Map<String, String> getDefaultParameters() {
    return defaultParameters;
  }

  public void setDefaultParameters(Map<String, String> defaultParameters) {
    this.defaultParameters = defaultParameters;
  }

  @Override
  public int hashCode() {
    return Objects.hash(defaultParameters);
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
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

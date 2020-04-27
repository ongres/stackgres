/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgpooling;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@RegisterForReflection
public class StackGresPoolingConfig extends CustomResource {

  private static final long serialVersionUID = 2719099984653736636L;

  @NotNull(message = "The specification is required")
  @Valid
  private StackGresPoolingConfigSpec spec;

  public StackGresPoolingConfig() {
    super(StackGresPoolingConfigDefinition.KIND);
  }

  public StackGresPoolingConfigSpec getSpec() {
    return spec;
  }

  public void setSpec(StackGresPoolingConfigSpec spec) {
    this.spec = spec;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("apiVersion", getApiVersion())
        .add("kind", getKind())
        .add("metadata", getMetadata())
        .add("spec", spec)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StackGresPoolingConfig that = (StackGresPoolingConfig) o;
    return Objects.equals(spec, that.spec);
  }

  @Override
  public int hashCode() {
    return Objects.hash(spec);
  }
}

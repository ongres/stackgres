/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgpooling;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
public class StackGresPoolingConfig extends CustomResource {

  private static final long serialVersionUID = 2719099984653736636L;

  @NotNull(message = "The specification is required")
  @Valid
  private StackGresPoolingConfigSpec spec;

  @Valid
  private StackGresPoolingConfigStatus status;

  public StackGresPoolingConfig() {
    super(StackGresPoolingConfigDefinition.KIND);
  }

  public StackGresPoolingConfigSpec getSpec() {
    return spec;
  }

  public void setSpec(StackGresPoolingConfigSpec spec) {
    this.spec = spec;
  }

  public StackGresPoolingConfigStatus getStatus() {
    return status;
  }

  public void setStatus(StackGresPoolingConfigStatus status) {
    this.status = status;
  }

  @Override
  public int hashCode() {
    return Objects.hash(spec, status);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresPoolingConfig)) {
      return false;
    }
    StackGresPoolingConfig other = (StackGresPoolingConfig) obj;
    return Objects.equals(spec, other.spec) && Objects.equals(status, other.status);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("apiVersion", getApiVersion())
        .add("kind", getKind())
        .add("metadata", getMetadata())
        .add("spec", spec)
        .add("status", status)
        .toString();
  }
}

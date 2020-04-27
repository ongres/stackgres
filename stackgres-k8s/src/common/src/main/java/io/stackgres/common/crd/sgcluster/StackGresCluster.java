/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@RegisterForReflection
public class StackGresCluster extends CustomResource {

  private static final long serialVersionUID = -5276087851826599719L;

  @JsonProperty("spec")
  @NotNull(message = "The specification is required")
  @Valid
  private StackGresClusterSpec spec;

  @JsonProperty("status")
  private StackGresClusterStatus status;

  public StackGresCluster() {
    super(StackGresClusterDefinition.KIND);
  }

  public StackGresClusterSpec getSpec() {
    return spec;
  }

  public void setSpec(StackGresClusterSpec spec) {
    this.spec = spec;
  }

  public StackGresClusterStatus getStatus() {
    return status;
  }

  public void setStatus(StackGresClusterStatus status) {
    this.status = status;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StackGresCluster that = (StackGresCluster) o;
    return Objects.equals(spec, that.spec)
        && Objects.equals(status, that.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(spec, status);
  }
}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgpgconfig;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
public class StackGresPostgresConfig extends CustomResource {

  private static final long serialVersionUID = -5276087851826599719L;

  @NotNull(message = "The specification is required")
  @Valid
  private StackGresPostgresConfigSpec spec;

  @Valid
  private StackGresPostgresConfigStatus status;

  public StackGresPostgresConfig() {
    super(StackGresPostgresConfigDefinition.KIND);
  }

  public StackGresPostgresConfigSpec getSpec() {
    return spec;
  }

  public void setSpec(StackGresPostgresConfigSpec spec) {
    this.spec = spec;
  }

  public StackGresPostgresConfigStatus getStatus() {
    return status;
  }

  public void setStatus(StackGresPostgresConfigStatus status) {
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
    if (!(obj instanceof StackGresPostgresConfig)) {
      return false;
    }
    StackGresPostgresConfig other = (StackGresPostgresConfig) obj;
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

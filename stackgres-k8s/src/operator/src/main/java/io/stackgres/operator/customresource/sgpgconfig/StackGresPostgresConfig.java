/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgpgconfig;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class StackGresPostgresConfig extends CustomResource {

  private static final long serialVersionUID = -5276087851826599719L;

  @NotNull(message = "The specification of StackGresPostgresConfig is required")
  @Valid
  private StackGresPostgresConfigSpec spec;

  public StackGresPostgresConfigSpec getSpec() {
    return spec;
  }

  public void setSpec(StackGresPostgresConfigSpec spec) {
    this.spec = spec;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("apiVersion", getApiVersion())
        .add("metadata", getMetadata())
        .add("spec", spec)
        .toString();
  }

}

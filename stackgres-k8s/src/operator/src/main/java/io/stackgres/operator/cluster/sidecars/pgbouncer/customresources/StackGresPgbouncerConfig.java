/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.sidecars.pgbouncer.customresources;

import javax.validation.constraints.NotNull;

import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class StackGresPgbouncerConfig extends CustomResource {

  private static final long serialVersionUID = 2719099984653736636L;

  @NotNull(message = "The specification of StackGresPgbouncerConfig is required")
  private StackGresPgbouncerConfigSpec spec;

  public StackGresPgbouncerConfigSpec getSpec() {
    return spec;
  }

  public void setSpec(StackGresPgbouncerConfigSpec spec) {
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

}

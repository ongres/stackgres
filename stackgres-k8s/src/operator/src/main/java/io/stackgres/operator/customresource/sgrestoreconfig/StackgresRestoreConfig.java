/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgrestoreconfig;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class StackgresRestoreConfig extends CustomResource {

  private static final long serialVersionUID = 1L;

  @NotNull(message = "The specification of StackgresRestoreConfigSpec is required")
  @Valid
  private StackgresRestoreConfigSpec spec;

  public StackgresRestoreConfigSpec getSpec() {
    return spec;
  }

  public void setSpec(StackgresRestoreConfigSpec spec) {
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

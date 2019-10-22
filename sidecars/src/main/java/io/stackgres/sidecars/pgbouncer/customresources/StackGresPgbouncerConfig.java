/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.sidecars.pgbouncer.customresources;

import com.google.common.base.MoreObjects;

import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class StackGresPgbouncerConfig extends CustomResource {

  private static final long serialVersionUID = 2719099984653736636L;

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
        .add("metadata", getMetadata())
        .add("spec", spec)
        .toString();
  }

}

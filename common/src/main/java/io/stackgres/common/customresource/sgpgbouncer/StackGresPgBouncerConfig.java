/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.customresource.sgpgbouncer;

import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.client.CustomResource;

public class StackGresPgBouncerConfig extends CustomResource {

  private static final long serialVersionUID = 1L;

  private StackGresPgBouncerConfigSpec spec;

  public StackGresPgBouncerConfigSpec getSpec() {
    return spec;
  }

  public void setSpec(StackGresPgBouncerConfigSpec spec) {
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

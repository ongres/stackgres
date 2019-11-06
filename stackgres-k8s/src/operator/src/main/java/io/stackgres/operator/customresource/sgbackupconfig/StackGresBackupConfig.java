/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgbackupconfig;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class StackGresBackupConfig extends CustomResource {

  private static final long serialVersionUID = 8062109585634644327L;

  @NotNull(message = "The specification of StackGresBackupConfig is required")
  @Valid
  private StackGresBackupConfigSpec spec;

  public StackGresBackupConfigSpec getSpec() {
    return spec;
  }

  public void setSpec(StackGresBackupConfigSpec spec) {
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

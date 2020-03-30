/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgbackup;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
public class StackGresBackup extends CustomResource {

  private static final long serialVersionUID = 8062109585634644327L;

  @NotNull(message = "The specification is required")
  @Valid
  private StackGresBackupSpec spec;

  @Valid
  private StackGresBackupStatus status;

  public StackGresBackup() {
    super(StackGresBackupDefinition.KIND);
  }

  public StackGresBackupSpec getSpec() {
    return spec;
  }

  public void setSpec(StackGresBackupSpec spec) {
    this.spec = spec;
  }

  public StackGresBackupStatus getStatus() {
    return status;
  }

  public void setStatus(StackGresBackupStatus status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("apiVersion", getApiVersion())
        .add("metadata", getMetadata())
        .add("spec", spec)
        .add("status", status)
        .toString();
  }

}

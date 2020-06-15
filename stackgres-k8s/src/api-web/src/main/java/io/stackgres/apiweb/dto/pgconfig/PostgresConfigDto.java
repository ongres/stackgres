/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.pgconfig;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.ResourceDto;

@RegisterForReflection
public class PostgresConfigDto extends ResourceDto {

  @NotNull(message = "The specification of postgres config is required")
  @Valid
  private PostgresConfigSpec spec;

  @Valid
  private PostgresConfigStatus status;

  public PostgresConfigSpec getSpec() {
    return spec;
  }

  public void setSpec(PostgresConfigSpec spec) {
    this.spec = spec;
  }

  public PostgresConfigStatus getStatus() {
    return status;
  }

  public void setStatus(PostgresConfigStatus status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("metadata", getMetadata())
        .add("spec", spec)
        .add("status", status)
        .toString();
  }

}

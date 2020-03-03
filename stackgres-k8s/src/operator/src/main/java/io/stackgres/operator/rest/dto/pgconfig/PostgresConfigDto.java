/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.dto.pgconfig;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.google.common.base.MoreObjects;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.operator.rest.dto.ResourceDto;

@RegisterForReflection
public class PostgresConfigDto extends ResourceDto {

  @NotNull(message = "The specification of postgres config is required")
  @Valid
  private PostgresConfigSpec spec;

  public PostgresConfigSpec getSpec() {
    return spec;
  }

  public void setSpec(PostgresConfigSpec spec) {
    this.spec = spec;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("metadata", getMetadata())
        .add("spec", spec)
        .toString();
  }

}

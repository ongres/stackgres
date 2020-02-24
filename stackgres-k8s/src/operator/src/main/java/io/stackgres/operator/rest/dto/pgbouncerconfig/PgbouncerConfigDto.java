/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.dto.pgbouncerconfig;

import javax.validation.constraints.NotNull;

import com.google.common.base.MoreObjects;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.operator.rest.dto.Resource;

@RegisterForReflection
public class PgbouncerConfigDto extends Resource {

  @NotNull(message = "The specification of pgbouncer config is required")
  private PgbouncerConfigSpec spec;

  public PgbouncerConfigSpec getSpec() {
    return spec;
  }

  public void setSpec(PgbouncerConfigSpec spec) {
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

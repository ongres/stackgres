/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.pooling;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.ResourceDto;

@RegisterForReflection
public class PoolingConfigDto extends ResourceDto {

  @NotNull(message = "The specification of pgbouncer config is required")
  @Valid
  private PoolingConfigSpec spec;

  @Valid
  private PoolingConfigStatus status;

  public PoolingConfigSpec getSpec() {
    return spec;
  }

  public void setSpec(PoolingConfigSpec spec) {
    this.spec = spec;
  }

  public PoolingConfigStatus getStatus() {
    return status;
  }

  public void setStatus(PoolingConfigStatus status) {
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

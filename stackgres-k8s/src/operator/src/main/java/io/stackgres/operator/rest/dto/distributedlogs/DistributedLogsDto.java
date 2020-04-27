/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.dto.distributedlogs;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.operator.rest.dto.ResourceDto;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class DistributedLogsDto extends ResourceDto {

  @JsonProperty("spec")
  @NotNull(message = "The specification is required")
  @Valid
  private DistributedLogsSpec spec;

  @JsonProperty("status")
  private DistributedLogsStatus status;

  public DistributedLogsSpec getSpec() {
    return spec;
  }

  public void setSpec(DistributedLogsSpec spec) {
    this.spec = spec;
  }

  public DistributedLogsStatus getStatus() {
    return status;
  }

  public void setStatus(DistributedLogsStatus status) {
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

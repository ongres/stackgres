/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.distributedlogs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DistributedLogsDto extends ResourceDto {

  @JsonProperty("spec")
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
    return StackGresUtil.toPrettyYaml(this);
  }

}

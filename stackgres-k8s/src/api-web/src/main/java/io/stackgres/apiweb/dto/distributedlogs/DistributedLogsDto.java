/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.distributedlogs;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.ResourceClassForDto;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@ResourceClassForDto(StackGresDistributedLogs.class)
public class DistributedLogsDto extends ResourceDto {

  private DistributedLogsSpec spec;

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

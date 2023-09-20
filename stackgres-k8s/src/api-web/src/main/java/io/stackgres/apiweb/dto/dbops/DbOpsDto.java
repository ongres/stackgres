/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.dbops;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.ResourceClassForDto;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@ResourceClassForDto(StackGresDbOps.class)
public class DbOpsDto extends ResourceDto {

  private DbOpsSpec spec;

  private DbOpsStatus status;

  public DbOpsSpec getSpec() {
    return spec;
  }

  public void setSpec(DbOpsSpec spec) {
    this.spec = spec;
  }

  public DbOpsStatus getStatus() {
    return status;
  }

  public void setStatus(DbOpsStatus status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

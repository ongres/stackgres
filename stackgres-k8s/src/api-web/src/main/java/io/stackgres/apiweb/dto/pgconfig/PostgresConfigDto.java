/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.pgconfig;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class PostgresConfigDto extends ResourceDto {

  private PostgresConfigSpec spec;

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
    return StackGresUtil.toPrettyYaml(this);
  }

}

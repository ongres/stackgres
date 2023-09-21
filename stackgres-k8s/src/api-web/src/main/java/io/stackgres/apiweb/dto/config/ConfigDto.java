/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.ResourceClassForDto;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgconfig.StackGresConfig;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@ResourceClassForDto(StackGresConfig.class)
public class ConfigDto extends ResourceDto {

  private ConfigSpec spec;

  private ConfigStatus status;

  public ConfigDto() {
    super();
  }

  public ConfigSpec getSpec() {
    return spec;
  }

  public void setSpec(ConfigSpec spec) {
    this.spec = spec;
  }

  public ConfigStatus getStatus() {
    return status;
  }

  public void setStatus(ConfigStatus status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

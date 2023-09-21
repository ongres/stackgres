/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.pooling;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.ResourceClassForDto;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@ResourceClassForDto(StackGresPoolingConfig.class)
public class PoolingConfigDto extends ResourceDto {

  private PoolingConfigSpec spec;

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
    return StackGresUtil.toPrettyYaml(this);
  }

}

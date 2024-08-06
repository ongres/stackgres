/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedcluster;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.postgres.service.PostgresService;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.CustomServicePort;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ShardedClusterPostgresCoordinatorServices {

  private PostgresService any;

  private PostgresService primary;

  private List<CustomServicePort> customPorts;

  public PostgresService getAny() {
    return any;
  }

  public void setAny(PostgresService any) {
    this.any = any;
  }

  public PostgresService getPrimary() {
    return primary;
  }

  public void setPrimary(PostgresService primary) {
    this.primary = primary;
  }

  public List<CustomServicePort> getCustomPorts() {
    return customPorts;
  }

  public void setCustomPorts(List<CustomServicePort> customPorts) {
    this.customPorts = customPorts;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

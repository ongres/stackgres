/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.distributedlogs;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.postgres.service.EnabledPostgresService;
import io.stackgres.apiweb.dto.postgres.service.PostgresService;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DistributedLogsPostgresServices {

  private EnabledPostgresService primary;

  private PostgresService replicas;

  public DistributedLogsPostgresServices() {}

  public DistributedLogsPostgresServices(EnabledPostgresService primary, PostgresService replicas) {
    this.primary = primary;
    this.replicas = replicas;
  }

  public EnabledPostgresService getPrimary() {
    return primary;
  }

  public void setPrimary(EnabledPostgresService primary) {
    this.primary = primary;
  }

  public PostgresService getReplicas() {
    return replicas;
  }

  public void setReplicas(PostgresService replicas) {
    this.replicas = replicas;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

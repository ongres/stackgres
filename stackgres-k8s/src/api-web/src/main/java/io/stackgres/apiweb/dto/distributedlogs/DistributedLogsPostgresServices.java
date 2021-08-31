/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.distributedlogs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.app.postgres.service.EnabledPostgresService;
import io.stackgres.apiweb.app.postgres.service.PostgresService;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class DistributedLogsPostgresServices implements KubernetesResource {

  private static final long serialVersionUID = 1L;

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

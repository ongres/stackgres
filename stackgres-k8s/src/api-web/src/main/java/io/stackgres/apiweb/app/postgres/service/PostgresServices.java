/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.app.postgres.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class PostgresServices implements KubernetesResource {

  private static final long serialVersionUID = 1L;

  private PostgresService primary;

  private PostgresService replicas;

  public PostgresServices() {}

  public PostgresServices(PostgresService primary, PostgresService replicas) {
    this.primary = primary;
    this.replicas = replicas;
  }

  public PostgresService getPrimary() {
    return primary;
  }

  public void setPrimary(PostgresService primary) {
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

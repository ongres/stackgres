/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.postgres.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class PostgresServices<P extends PostgresService, R extends PostgresService> {

  private P primary;

  private R replicas;

  public PostgresService getPrimary() {
    return primary;
  }

  public void setPrimary(P primary) {
    this.primary = primary;
  }

  public R getReplicas() {
    return replicas;
  }

  public void setReplicas(R replicas) {
    this.replicas = replicas;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

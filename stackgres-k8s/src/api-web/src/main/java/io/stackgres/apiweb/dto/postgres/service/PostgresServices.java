/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.postgres.service;

import java.util.Objects;

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
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PostgresServices<?, ?> that = (PostgresServices<?, ?>) o;
    return Objects.equals(primary, that.primary)
        && Objects.equals(replicas, that.replicas);
  }

  @Override
  public int hashCode() {
    return Objects.hash(primary, replicas);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

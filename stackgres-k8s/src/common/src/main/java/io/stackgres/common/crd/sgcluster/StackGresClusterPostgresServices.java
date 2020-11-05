/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresClusterPostgresServices {

  private StackGresClusterPostgresService primary;

  private StackGresClusterPostgresService replicas;

  public StackGresClusterPostgresService getPrimary() {
    return primary;
  }

  public void setPrimary(StackGresClusterPostgresService primary) {
    this.primary = primary;
  }

  public StackGresClusterPostgresService getReplicas() {
    return replicas;
  }

  public void setReplicas(StackGresClusterPostgresService replicas) {
    this.replicas = replicas;
  }

  @Override
  public int hashCode() {
    return Objects.hash(primary, replicas);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StackGresClusterPostgresServices that = (StackGresClusterPostgresServices) o;
    return Objects.equals(primary, that.primary) && Objects.equals(replicas, that.replicas);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

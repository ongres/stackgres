/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterPostgresServices {

  private ClusterPostgresService primary;

  private ClusterPostgresService replicas;

  public ClusterPostgresService getPrimary() {
    return primary;
  }

  public void setPrimary(ClusterPostgresService primary) {
    this.primary = primary;
  }

  public ClusterPostgresService getReplicas() {
    return replicas;
  }

  public void setReplicas(ClusterPostgresService replicas) {
    this.replicas = replicas;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

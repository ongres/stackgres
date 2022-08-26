/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterReplicateFrom {

  @JsonProperty("instance")
  private ClusterReplicateFromInstance instance;

  public ClusterReplicateFromInstance getInstance() {
    return instance;
  }

  public void setInstance(ClusterReplicateFromInstance instance) {
    this.instance = instance;
  }

  @Override
  public int hashCode() {
    return Objects.hash(instance);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ClusterReplicateFrom)) {
      return false;
    }
    ClusterReplicateFrom other = (ClusterReplicateFrom) obj;
    return Objects.equals(instance, other.instance);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

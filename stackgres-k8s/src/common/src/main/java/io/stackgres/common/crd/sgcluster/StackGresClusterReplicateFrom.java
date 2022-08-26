/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresClusterReplicateFrom {

  @JsonProperty("instance")
  @Valid
  @NotNull(message = "instance section is required")
  private StackGresClusterReplicateFromInstance instance;

  public StackGresClusterReplicateFromInstance getInstance() {
    return instance;
  }

  public void setInstance(StackGresClusterReplicateFromInstance instance) {
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
    if (!(obj instanceof StackGresClusterReplicateFrom)) {
      return false;
    }
    StackGresClusterReplicateFrom other = (StackGresClusterReplicateFrom) obj;
    return Objects.equals(instance, other.instance);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

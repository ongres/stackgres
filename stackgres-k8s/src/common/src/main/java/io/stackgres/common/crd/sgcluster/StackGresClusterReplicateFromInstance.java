/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresClusterReplicateFromInstance {

  @JsonProperty("external")
  @Valid
  @NotNull(message = "external section is required")
  private StackGresClusterReplicateFromExternal external;

  public StackGresClusterReplicateFromExternal getExternal() {
    return external;
  }

  public void setExternal(StackGresClusterReplicateFromExternal external) {
    this.external = external;
  }

  @Override
  public int hashCode() {
    return Objects.hash(external);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterReplicateFromInstance)) {
      return false;
    }
    StackGresClusterReplicateFromInstance other = (StackGresClusterReplicateFromInstance) obj;
    return Objects.equals(external, other.external);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

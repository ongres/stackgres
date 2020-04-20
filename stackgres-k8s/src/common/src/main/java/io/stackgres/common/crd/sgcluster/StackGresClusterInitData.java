/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresClusterInitData {

  @JsonProperty("restore")
  private ClusterRestore restore;

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("restore", getRestore())
        .toString();
  }

  public ClusterRestore getRestore() {
    return restore;
  }

  public void setRestore(ClusterRestore restore) {
    this.restore = restore;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StackGresClusterInitData that = (StackGresClusterInitData) o;
    return Objects.equals(restore, that.restore);
  }

  @Override
  public int hashCode() {
    return Objects.hash(restore);
  }
}

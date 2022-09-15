/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ClusterInitData {

  private ClusterRestore restore;

  public ClusterRestore getRestore() {
    return restore;
  }

  public void setRestore(ClusterRestore restore) {
    this.restore = restore;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClusterInitData that = (ClusterInitData) o;
    return Objects.equals(restore, that.restore);
  }

  @Override
  public int hashCode() {
    return Objects.hash(restore);
  }
}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.List;
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
  private StackGresClusterRestore restore;

  @JsonProperty("scripts")
  private List<StackGresClusterScriptEntry> scripts;

  public StackGresClusterRestore getRestore() {
    return restore;
  }

  public void setRestore(StackGresClusterRestore restore) {
    this.restore = restore;
  }

  public List<StackGresClusterScriptEntry> getScripts() {
    return scripts;
  }

  public void setScripts(List<StackGresClusterScriptEntry> scripts) {
    this.scripts = scripts;
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
    return Objects.equals(restore, that.restore)
        && Objects.equals(scripts, that.scripts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(restore, scripts);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("restore", restore)
        .add("scripts", scripts)
        .toString();
  }
}

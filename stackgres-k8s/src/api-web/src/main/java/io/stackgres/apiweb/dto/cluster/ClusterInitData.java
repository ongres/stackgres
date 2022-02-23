/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.List;
import java.util.Objects;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.script.ScriptEntry;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterInitData {

  @Valid
  private ClusterRestore restore;

  @Valid
  private List<ScriptEntry> scripts;

  public ClusterRestore getRestore() {
    return restore;
  }

  public void setRestore(ClusterRestore restore) {
    this.restore = restore;
  }

  public List<ScriptEntry> getScripts() {
    return scripts;
  }

  public void setScripts(List<ScriptEntry> scripts) {
    this.scripts = scripts;
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
    return Objects.equals(restore, that.restore)
        && Objects.equals(scripts, that.scripts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(restore, scripts);
  }
}

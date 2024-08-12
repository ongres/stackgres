/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ClusterManagedSqlStatus {

  private Integer lastId;

  private List<ClusterManagedScriptEntryStatus> scripts;

  public Integer getLastId() {
    return lastId;
  }

  public void setLastId(Integer lastId) {
    this.lastId = lastId;
  }

  public List<ClusterManagedScriptEntryStatus> getScripts() {
    return scripts;
  }

  public void setScripts(List<ClusterManagedScriptEntryStatus> scripts) {
    this.scripts = scripts;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ClusterManagedSql {

  @JsonProperty("continueOnSGScriptError")
  private Boolean continueOnScriptError;

  @JsonProperty("scripts")
  private List<ClusterManagedScriptEntry> scripts;

  public Boolean getContinueOnScriptError() {
    return continueOnScriptError;
  }

  public void setContinueOnScriptError(Boolean continueOnScriptError) {
    this.continueOnScriptError = continueOnScriptError;
  }

  public List<ClusterManagedScriptEntry> getScripts() {
    return scripts;
  }

  public void setScripts(List<ClusterManagedScriptEntry> scripts) {
    this.scripts = scripts;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

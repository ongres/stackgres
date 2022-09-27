/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.script;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ScriptSpec {

  @JsonProperty("managedVersions")
  private Boolean managedVersions;

  @JsonProperty("continueOnError")
  private Boolean continueOnError;

  @JsonProperty("scripts")
  private List<ScriptEntry> scripts;

  public Boolean isManagedVersions() {
    return managedVersions;
  }

  public void setManagedVersions(Boolean managedVersions) {
    this.managedVersions = managedVersions;
  }

  public Boolean isContinueOnError() {
    return continueOnError;
  }

  public void setContinueOnError(Boolean continueOnError) {
    this.continueOnError = continueOnError;
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

}

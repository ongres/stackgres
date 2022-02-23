/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.script;

import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ScriptSpec {

  @JsonProperty("managedVersions")
  private Boolean managedVersions;

  @JsonProperty("continueOnError")
  private Boolean continueOnError;

  @JsonProperty("scripts")
  @NotNull(message = "scripts section cannot be null")
  @Valid
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
  public int hashCode() {
    return Objects.hash(continueOnError, managedVersions, scripts);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ScriptSpec)) {
      return false;
    }
    ScriptSpec other = (ScriptSpec) obj;
    return Objects.equals(continueOnError, other.continueOnError)
        && Objects.equals(managedVersions, other.managedVersions)
        && Objects.equals(scripts, other.scripts);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

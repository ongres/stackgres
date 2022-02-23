/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgscript;

import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresScriptSpec implements KubernetesResource {

  private static final long serialVersionUID = -1L;

  @JsonProperty("managedVersions")
  private Boolean managedVersions;

  @JsonProperty("continueOnError")
  private Boolean continueOnError;

  @JsonProperty("scripts")
  @NotNull(message = "scripts section cannot be null")
  @Valid
  private List<StackGresScriptEntry> scripts;

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

  public List<StackGresScriptEntry> getScripts() {
    return scripts;
  }

  public void setScripts(List<StackGresScriptEntry> scripts) {
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
    if (!(obj instanceof StackGresScriptSpec)) {
      return false;
    }
    StackGresScriptSpec other = (StackGresScriptSpec) obj;
    return Objects.equals(continueOnError, other.continueOnError)
        && Objects.equals(managedVersions, other.managedVersions)
        && Objects.equals(scripts, other.scripts);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

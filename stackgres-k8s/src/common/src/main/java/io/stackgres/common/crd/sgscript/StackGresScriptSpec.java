/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgscript;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresScriptSpec {

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

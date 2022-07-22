/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.List;
import java.util.Objects;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresClusterManagedSql {

  @JsonProperty("continueOnSGScriptError")
  private Boolean continueOnScriptError;

  @JsonProperty("scripts")
  @Valid
  private List<StackGresClusterManagedScriptEntry> scripts;

  public Boolean getContinueOnScriptError() {
    return continueOnScriptError;
  }

  public void setContinueOnScriptError(Boolean continueOnScriptError) {
    this.continueOnScriptError = continueOnScriptError;
  }

  public List<StackGresClusterManagedScriptEntry> getScripts() {
    return scripts;
  }

  public void setScripts(List<StackGresClusterManagedScriptEntry> scripts) {
    this.scripts = scripts;
  }

  @Override
  public int hashCode() {
    return Objects.hash(continueOnScriptError, scripts);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterManagedSql)) {
      return false;
    }
    StackGresClusterManagedSql other = (StackGresClusterManagedSql) obj;
    return Objects.equals(continueOnScriptError, other.continueOnScriptError)
        && Objects.equals(scripts, other.scripts);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

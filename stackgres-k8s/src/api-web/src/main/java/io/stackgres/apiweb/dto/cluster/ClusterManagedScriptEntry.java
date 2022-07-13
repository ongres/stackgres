/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.script.ScriptSpec;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterManagedScriptEntry {

  @JsonProperty("id")
  private Integer id;

  @JsonProperty("sgScript")
  private String sgScript;

  @JsonProperty("scriptSpec")
  private ScriptSpec scriptSpec;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getSgScript() {
    return sgScript;
  }

  public void setSgScript(String sgScript) {
    this.sgScript = sgScript;
  }

  public ScriptSpec getScriptSpec() {
    return scriptSpec;
  }

  public void setScriptSpec(ScriptSpec scriptSpec) {
    this.scriptSpec = scriptSpec;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, scriptSpec, sgScript);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ClusterManagedScriptEntry)) {
      return false;
    }
    ClusterManagedScriptEntry other = (ClusterManagedScriptEntry) obj;
    return Objects.equals(id, other.id)
        && Objects.equals(scriptSpec, other.scriptSpec) && Objects.equals(sgScript, other.sgScript);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

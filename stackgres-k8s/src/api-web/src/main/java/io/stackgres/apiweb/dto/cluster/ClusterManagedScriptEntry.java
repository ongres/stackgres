/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.script.ScriptSpec;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
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
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

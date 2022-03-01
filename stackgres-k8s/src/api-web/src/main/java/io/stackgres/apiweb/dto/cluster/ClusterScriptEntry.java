/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterScriptEntry {

  @NotEmpty
  private String name;

  @NotEmpty
  private String database;

  @NotEmpty
  private String script;

  @Valid
  private ClusterScriptFrom scriptFrom;

  @JsonIgnore
  @AssertTrue(message = "script and scriptFrom are mutually exclusive and one of them is required.")
  public boolean areScriptAndScriptFromMutuallyExclusiveAndOneRequired() {
    return (script != null && scriptFrom == null) // NOPMD
        || (script == null && scriptFrom != null); // NOPMD
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDatabase() {
    return database;
  }

  public void setDatabase(String database) {
    this.database = database;
  }

  public String getScript() {
    return script;
  }

  public void setScript(String script) {
    this.script = script;
  }

  public ClusterScriptFrom getScriptFrom() {
    return scriptFrom;
  }

  public void setScriptFrom(ClusterScriptFrom scriptFrom) {
    this.scriptFrom = scriptFrom;
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
    ClusterScriptEntry that = (ClusterScriptEntry) o;
    return Objects.equals(name, that.name)
        && Objects.equals(database, that.database)
        && Objects.equals(script, that.script)
        && Objects.equals(scriptFrom, that.scriptFrom);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, database, script, scriptFrom);
  }
}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import javax.validation.constraints.AssertTrue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresClusterScriptEntry {

  @JsonProperty("name")
  private String name;

  @JsonProperty("database")
  private String database;

  @JsonProperty("script")
  private String script;

  @JsonProperty("scriptFrom")
  private StackGresClusterScriptFrom scriptFrom;

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

  public StackGresClusterScriptFrom getScriptFrom() {
    return scriptFrom;
  }

  public void setScriptFrom(StackGresClusterScriptFrom scriptFrom) {
    this.scriptFrom = scriptFrom;
  }

  @Override
  public int hashCode() {
    return Objects.hash(database, name, script, scriptFrom);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterScriptEntry)) {
      return false;
    }
    StackGresClusterScriptEntry other = (StackGresClusterScriptEntry) obj;
    return Objects.equals(database, other.database) && Objects.equals(name, other.name)
        && Objects.equals(script, other.script) && Objects.equals(scriptFrom, other.scriptFrom);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("database", database)
        .add("script", script)
        .add("scriptFrom", scriptFrom)
        .toString();
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterScriptEntry {

  private String name;

  private String database;

  private String script;

  private ClusterScriptFrom scriptFrom;

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

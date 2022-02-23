/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.script;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ScriptEntry {

  private String name;

  private Integer id;

  private Integer version;

  private String database;

  private String user;

  private String script;

  private ScriptFrom scriptFrom;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public String getDatabase() {
    return database;
  }

  public void setDatabase(String database) {
    this.database = database;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getScript() {
    return script;
  }

  public void setScript(String script) {
    this.script = script;
  }

  public ScriptFrom getScriptFrom() {
    return scriptFrom;
  }

  public void setScriptFrom(ScriptFrom scriptFrom) {
    this.scriptFrom = scriptFrom;
  }

  @Override
  public int hashCode() {
    return Objects.hash(database, id, name, script, scriptFrom, user, version);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ScriptEntry)) {
      return false;
    }
    ScriptEntry other = (ScriptEntry) obj;
    return Objects.equals(database, other.database) && Objects.equals(id, other.id)
        && Objects.equals(name, other.name) && Objects.equals(script, other.script)
        && Objects.equals(scriptFrom, other.scriptFrom) && Objects.equals(user, other.user)
        && Objects.equals(version, other.version);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgscript;

import java.util.Objects;
import java.util.Optional;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresScriptEntry {

  @JsonProperty("name")
  private String name;

  @JsonProperty("id")
  @NotNull(message = "id cannot be null")
  private Integer id;

  @JsonProperty("version")
  @NotNull(message = "version cannot be null")
  private Integer version;

  @JsonProperty("database")
  private String database;

  @JsonProperty("user")
  private String user;

  @JsonProperty("script")
  private String script;

  @JsonProperty("scriptFrom")
  @Valid
  private StackGresScriptFrom scriptFrom;

  @ReferencedField("database")
  interface Database extends FieldReference { }

  @ReferencedField("script")
  interface Script extends FieldReference { }

  @ReferencedField("scriptFrom")
  interface ScriptFrom extends FieldReference { }

  @JsonIgnore
  @AssertTrue(message = "script and scriptFrom are mutually exclusive and required.",
      payload = { Script.class, ScriptFrom.class })
  public boolean isScriptMutuallyExclusiveAndRequired() {
    return (script != null && scriptFrom == null) // NOPMD
        || (script == null && scriptFrom != null); // NOPMD
  }

  @JsonIgnore
  @AssertTrue(message = "database name must not be empty.",
      payload = Database.class)
  public boolean isDatabaseNameNonEmpty() {
    return database == null || !database.isEmpty();
  }

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

  @JsonIgnore
  public String getDatabaseOrDefault() {
    return Optional.ofNullable(database).orElse("postgres");
  }

  public void setDatabase(String database) {
    this.database = database;
  }

  public String getUser() {
    return user;
  }

  @JsonIgnore
  public String getUserOrDefault() {
    return Optional.ofNullable(user).orElse("postgres");
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

  public StackGresScriptFrom getScriptFrom() {
    return scriptFrom;
  }

  public void setScriptFrom(StackGresScriptFrom scriptFrom) {
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
    if (!(obj instanceof StackGresScriptEntry)) {
      return false;
    }
    StackGresScriptEntry other = (StackGresScriptEntry) obj;
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

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.script;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ScriptEntry {

  private String name;

  private Integer id;

  private Integer version;

  private String database;

  private String user;

  @JsonProperty("wrapInTransaction")
  private String wrapInTransaction;

  @JsonProperty("storeStatusInDatabase")
  private Boolean storeStatusInDatabase;

  @JsonProperty("retryOnError")
  private Boolean retryOnError;

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

  public String getWrapInTransaction() {
    return wrapInTransaction;
  }

  public void setWrapInTransaction(String wrapInTransaction) {
    this.wrapInTransaction = wrapInTransaction;
  }

  public Boolean getStoreStatusInDatabase() {
    return storeStatusInDatabase;
  }

  public void setStoreStatusInDatabase(Boolean storeStatusInDatabase) {
    this.storeStatusInDatabase = storeStatusInDatabase;
  }

  public Boolean getRetryOnError() {
    return retryOnError;
  }

  public void setRetryOnError(Boolean retryOnError) {
    this.retryOnError = retryOnError;
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
    return Objects.hash(database, id, name, retryOnError, script, scriptFrom, storeStatusInDatabase,
        user, version, wrapInTransaction);
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
        && Objects.equals(name, other.name) && Objects.equals(retryOnError, other.retryOnError)
        && Objects.equals(script, other.script) && Objects.equals(scriptFrom, other.scriptFrom)
        && Objects.equals(storeStatusInDatabase, other.storeStatusInDatabase)
        && Objects.equals(user, other.user) && Objects.equals(version, other.version)
        && Objects.equals(wrapInTransaction, other.wrapInTransaction);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.script;

import com.fasterxml.jackson.annotation.JsonInclude;
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

  private String wrapInTransaction;

  private Boolean storeStatusInDatabase;

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
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

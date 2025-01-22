/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgscript;

import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_DATABASE;

import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.stackgres.common.validation.ValidEnum;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresScriptEntry {

  private String name;

  @NotNull(message = "id cannot be null")
  private Integer id;

  @NotNull(message = "version cannot be null")
  private Integer version;

  private String database;

  @ValidEnum(enumClass = StackGresScriptTransactionIsolationLevel.class, allowNulls = true,
      message = "wrapInTransaction must be read-committed, repeatable-read or serializable")
  private String wrapInTransaction;

  private Boolean storeStatusInDatabase;

  private Boolean retryOnError;

  private String user;

  private String script;

  @Valid
  private StackGresScriptFrom scriptFrom;

  @ReferencedField("database")
  interface Database extends FieldReference { }

  @ReferencedField("script")
  interface Script extends FieldReference { }

  @ReferencedField("scriptFrom")
  interface ScriptFrom extends FieldReference { }

  @ReferencedField("storeStatusInDatabase")
  interface StoreStatusInDatabase extends FieldReference { }

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

  @JsonIgnore
  @AssertTrue(message = "Can not set storeStatusInDatabase when wrapInTransaction is not set.",
      payload = StoreStatusInDatabase.class)
  public boolean isWrapInTransactionSetWhenStoreStatusInDatabaseIsSet() {
    return storeStatusInDatabase == null || !storeStatusInDatabase || wrapInTransaction != null;
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
    return Optional.ofNullable(database).orElse(SUPERUSER_DATABASE);
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

  @JsonIgnore
  public boolean getStoreStatusInDatabaseOrDefault() {
    return Optional.ofNullable(storeStatusInDatabase).orElse(false);
  }

  public void setStoreStatusInDatabase(Boolean storeStatusInDatabase) {
    this.storeStatusInDatabase = storeStatusInDatabase;
  }

  public Boolean getRetryOnError() {
    return retryOnError;
  }

  @JsonIgnore
  public boolean getRetryOnErrorOrDefault() {
    return Optional.ofNullable(retryOnError).orElse(false);
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

  public StackGresScriptFrom getScriptFrom() {
    return scriptFrom;
  }

  public void setScriptFrom(StackGresScriptFrom scriptFrom) {
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
    if (!(obj instanceof StackGresScriptEntry)) {
      return false;
    }
    StackGresScriptEntry other = (StackGresScriptEntry) obj;
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

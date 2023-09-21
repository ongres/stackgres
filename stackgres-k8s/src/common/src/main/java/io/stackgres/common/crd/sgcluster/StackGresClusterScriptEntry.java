/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterScriptEntry {

  private String name;

  private String database;

  private String script;

  @Valid
  private StackGresClusterScriptFrom scriptFrom;

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
    return StackGresUtil.toPrettyYaml(this);
  }

}

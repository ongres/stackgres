/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresDbOpsPgbenchCustomScript {

  private String script;

  @Valid
  private StackGresDbOpsPgbenchCustomScriptFrom scriptFrom;

  private String builtin;

  private Integer weight;

  @ReferencedField("script")
  interface Script extends FieldReference { }

  @ReferencedField("scriptFrom")
  interface ScriptFrom extends FieldReference { }

  @ReferencedField("builtin")
  interface Builtin extends FieldReference { }

  @JsonIgnore
  @AssertTrue(message = "script, scriptFrom and builtin are mutually exclusive and required.",
      payload = { Script.class, ScriptFrom.class, Builtin.class })
  public boolean isScriptMutuallyExclusiveAndRequired() {
    return (script != null && scriptFrom == null && builtin == null) // NOPMD
        || (script == null && scriptFrom != null && builtin == null) // NOPMD
        || (script == null && scriptFrom == null && builtin != null); // NOPMD
  }

  public String getScript() {
    return script;
  }

  public void setScript(String script) {
    this.script = script;
  }

  public StackGresDbOpsPgbenchCustomScriptFrom getScriptFrom() {
    return scriptFrom;
  }

  public void setScriptFrom(StackGresDbOpsPgbenchCustomScriptFrom scriptFrom) {
    this.scriptFrom = scriptFrom;
  }

  public String getBuiltin() {
    return builtin;
  }

  public void setBuiltin(String builtin) {
    this.builtin = builtin;
  }

  public Integer getWeight() {
    return weight;
  }

  public void setWeight(Integer weight) {
    this.weight = weight;
  }

  @Override
  public int hashCode() {
    return Objects.hash(builtin, script, scriptFrom, weight);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDbOpsPgbenchCustomScript)) {
      return false;
    }
    StackGresDbOpsPgbenchCustomScript other = (StackGresDbOpsPgbenchCustomScript) obj;
    return Objects.equals(builtin, other.builtin) && Objects.equals(script, other.script)
        && Objects.equals(scriptFrom, other.scriptFrom) && Objects.equals(weight, other.weight);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

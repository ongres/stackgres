/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgstream;

import java.util.Objects;

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

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresStreamTargetPgLambda {

  @ValidEnum(enumClass = StreamTargetPgLambdaScriptType.class, allowNulls = true,
      message = "scriptType must be javascript")
  private String scriptType;

  private String script;

  @Valid
  private StackGresStreamTargetPgLambdaScriptFrom scriptFrom;

  @Valid
  private StackGresStreamTargetPgLambdaKnative knative;

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

  public String getScriptType() {
    return scriptType;
  }

  public void setScriptType(String scriptType) {
    this.scriptType = scriptType;
  }

  public String getScript() {
    return script;
  }

  public void setScript(String script) {
    this.script = script;
  }

  public StackGresStreamTargetPgLambdaScriptFrom getScriptFrom() {
    return scriptFrom;
  }

  public void setScriptFrom(StackGresStreamTargetPgLambdaScriptFrom scriptFrom) {
    this.scriptFrom = scriptFrom;
  }

  public StackGresStreamTargetPgLambdaKnative getKnative() {
    return knative;
  }

  public void setKnative(StackGresStreamTargetPgLambdaKnative knative) {
    this.knative = knative;
  }

  @Override
  public int hashCode() {
    return Objects.hash(knative, script, scriptFrom, scriptType);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresStreamTargetPgLambda)) {
      return false;
    }
    StackGresStreamTargetPgLambda other = (StackGresStreamTargetPgLambda) obj;
    return Objects.equals(knative, other.knative) && Objects.equals(script, other.script)
        && Objects.equals(scriptFrom, other.scriptFrom)
        && Objects.equals(scriptType, other.scriptType);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

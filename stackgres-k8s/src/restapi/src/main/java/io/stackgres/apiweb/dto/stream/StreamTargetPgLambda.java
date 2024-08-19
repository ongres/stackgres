/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.stream;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class StreamTargetPgLambda {

  private String scriptType;

  private String script;

  private StreamTargetPgLambdaScriptFrom scriptFrom;

  private StreamTargetPgLambdaKnative knative;

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

  public StreamTargetPgLambdaScriptFrom getScriptFrom() {
    return scriptFrom;
  }

  public void setScriptFrom(StreamTargetPgLambdaScriptFrom scriptFrom) {
    this.scriptFrom = scriptFrom;
  }

  public StreamTargetPgLambdaKnative getKnative() {
    return knative;
  }

  public void setKnative(StreamTargetPgLambdaKnative knative) {
    this.knative = knative;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

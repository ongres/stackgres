/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.constraints.AssertTrue;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterReplicateFromCustomRestoreMethod {

  private String command;

  private String script;

  @JsonProperty("no_leader")
  private Boolean noLeader;

  @JsonProperty("keep_data")
  private Boolean keepData;

  @JsonProperty("no_params")
  private Boolean noParams;

  private Map<String, String> parameters;

  @JsonProperty("keep_existing_recovery_conf")
  private Boolean keepExistingRecoveryConf;

  @JsonProperty("recovery_conf")
  private Map<String, String> recoveryConf;

  @ReferencedField("command")
  interface Command extends FieldReference { }

  @ReferencedField("script")
  interface Script extends FieldReference { }

  @JsonIgnore
  @AssertTrue(message = "One of command or script is required",
      payload = { Command.class, Script.class })
  public boolean isCommandOrScriptNotNull() {
    return !(command == null
        && script == null);
  }

  @JsonIgnore
  @AssertTrue(message = "command and script are mutually exclusive",
      payload = { Command.class, Script.class })
  public boolean isCommandOrScriptMutuallyExclusive() {
    return (script == null && command == null)
        || (script == null && command != null)
        || (script != null && command == null);
  }

  public String getCommand() {
    return command;
  }

  public void setCommand(String command) {
    this.command = command;
  }

  public String getScript() {
    return script;
  }

  public void setScript(String script) {
    this.script = script;
  }

  public Boolean getNoLeader() {
    return noLeader;
  }

  public void setNoLeader(Boolean noLeader) {
    this.noLeader = noLeader;
  }

  public Boolean getKeepData() {
    return keepData;
  }

  public void setKeepData(Boolean keepData) {
    this.keepData = keepData;
  }

  public Boolean getNoParams() {
    return noParams;
  }

  public void setNoParams(Boolean noParams) {
    this.noParams = noParams;
  }

  public Map<String, String> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, String> parameters) {
    this.parameters = parameters;
  }

  public Boolean getKeepExistingRecoveryConf() {
    return keepExistingRecoveryConf;
  }

  public void setKeepExistingRecoveryConf(Boolean keepExistingRecoveryConf) {
    this.keepExistingRecoveryConf = keepExistingRecoveryConf;
  }

  public Map<String, String> getRecoveryConf() {
    return recoveryConf;
  }

  public void setRecoveryConf(Map<String, String> recoveryConf) {
    this.recoveryConf = recoveryConf;
  }

  @Override
  public int hashCode() {
    return Objects.hash(command, keepData, noLeader, noParams, parameters, script);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterReplicateFromCustomRestoreMethod)) {
      return false;
    }
    StackGresClusterReplicateFromCustomRestoreMethod other = (StackGresClusterReplicateFromCustomRestoreMethod) obj;
    return Objects.equals(command, other.command) && Objects.equals(keepData, other.keepData)
        && Objects.equals(noLeader, other.noLeader) && Objects.equals(noParams, other.noParams)
        && Objects.equals(parameters, other.parameters) && Objects.equals(script, other.script);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

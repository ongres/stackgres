/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ClusterReplicateFromCustomRestoreMethod {

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
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

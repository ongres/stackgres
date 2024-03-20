/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.external.shardingsphere;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class ComputeNodeBootstrap {

  private ComputeNodeAgentConfig agentConfig;

  private String logbackConfig;

  private ComputeNodeServerConfig serverConfig;

  public ComputeNodeAgentConfig getAgentConfig() {
    return agentConfig;
  }

  public void setAgentConfig(ComputeNodeAgentConfig agentConfig) {
    this.agentConfig = agentConfig;
  }

  public String getLogbackConfig() {
    return logbackConfig;
  }

  public void setLogbackConfig(String logbackConfig) {
    this.logbackConfig = logbackConfig;
  }

  public ComputeNodeServerConfig getServerConfig() {
    return serverConfig;
  }

  public void setServerConfig(ComputeNodeServerConfig serverConfig) {
    this.serverConfig = serverConfig;
  }

  @Override
  public int hashCode() {
    return Objects.hash(agentConfig, logbackConfig, serverConfig);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ComputeNodeBootstrap)) {
      return false;
    }
    ComputeNodeBootstrap other = (ComputeNodeBootstrap) obj;
    return Objects.equals(agentConfig, other.agentConfig) && Objects.equals(logbackConfig, other.logbackConfig)
        && Objects.equals(serverConfig, other.serverConfig);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

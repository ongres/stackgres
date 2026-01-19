/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ClusterPatroni {

  private Boolean connectUsingFqdn;

  private Map<String, Object> dynamicConfig;

  private Map<String, Object> initialConfig;

  public Boolean getConnectUsingFqdn() {
    return connectUsingFqdn;
  }

  public void setConnectUsingFqdn(Boolean connectUsingFqdn) {
    this.connectUsingFqdn = connectUsingFqdn;
  }

  public Map<String, Object> getDynamicConfig() {
    return dynamicConfig;
  }

  public void setDynamicConfig(Map<String, Object> dynamicConfig) {
    this.dynamicConfig = dynamicConfig;
  }

  public Map<String, Object> getInitialConfig() {
    return initialConfig;
  }

  public void setInitialConfig(Map<String, Object> initialConfig) {
    this.initialConfig = initialConfig;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

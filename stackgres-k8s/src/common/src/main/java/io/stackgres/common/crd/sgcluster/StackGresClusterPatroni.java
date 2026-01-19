/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StackGresClusterPatroni {

  private Boolean connectUsingFqdn;

  private StackGresClusterPatroniDynamicConfig dynamicConfig;

  private StackGresClusterPatroniConfig initialConfig;

  public Boolean getConnectUsingFqdn() {
    return connectUsingFqdn;
  }

  public void setConnectUsingFqdn(Boolean connectUsingFqdn) {
    this.connectUsingFqdn = connectUsingFqdn;
  }

  public StackGresClusterPatroniDynamicConfig getDynamicConfig() {
    return dynamicConfig;
  }

  public void setDynamicConfig(StackGresClusterPatroniDynamicConfig dynamicConfig) {
    this.dynamicConfig = dynamicConfig;
  }

  public StackGresClusterPatroniConfig getInitialConfig() {
    return initialConfig;
  }

  public void setInitialConfig(StackGresClusterPatroniConfig initialConfig) {
    this.initialConfig = initialConfig;
  }

  @Override
  public int hashCode() {
    return Objects.hash(connectUsingFqdn, dynamicConfig, initialConfig);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterPatroni)) {
      return false;
    }
    StackGresClusterPatroni other = (StackGresClusterPatroni) obj;
    return Objects.equals(connectUsingFqdn, other.connectUsingFqdn)
        && Objects.equals(dynamicConfig, other.dynamicConfig)
        && Objects.equals(initialConfig, other.initialConfig);
  }

  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

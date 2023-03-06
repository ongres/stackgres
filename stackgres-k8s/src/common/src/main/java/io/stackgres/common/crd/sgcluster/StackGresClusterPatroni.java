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

  private StackGresClusterPatroniInitialConfig initialConfig;

  public StackGresClusterPatroniInitialConfig getInitialConfig() {
    return initialConfig;
  }

  public void setInitialConfig(StackGresClusterPatroniInitialConfig initialConfig) {
    this.initialConfig = initialConfig;
  }

  @Override
  public int hashCode() {
    return Objects.hash(initialConfig);
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
    return Objects.equals(initialConfig, other.initialConfig);
  }

  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

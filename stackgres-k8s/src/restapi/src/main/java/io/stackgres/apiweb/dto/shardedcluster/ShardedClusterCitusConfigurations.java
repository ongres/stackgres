/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedcluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.AdditionalProperties;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ShardedClusterCitusConfigurations extends AdditionalProperties {

  private String updateNodeInterval;

  private Boolean enableNodeAutoRemoval;

  public String getUpdateNodeInterval() {
    return updateNodeInterval;
  }

  public void setUpdateNodeInterval(String updateNodeInterval) {
    this.updateNodeInterval = updateNodeInterval;
  }

  public Boolean getEnableNodeAutoRemoval() {
    return enableNodeAutoRemoval;
  }

  public void setEnableNodeAutoRemoval(Boolean enableNodeAutoRemoval) {
    this.enableNodeAutoRemoval = enableNodeAutoRemoval;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

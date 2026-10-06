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

  private Boolean autoReplicateReferenceTables;

  private Boolean connectToPooler;

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

  public Boolean getAutoReplicateReferenceTables() {
    return autoReplicateReferenceTables;
  }

  public void setAutoReplicateReferenceTables(Boolean autoReplicateReferenceTables) {
    this.autoReplicateReferenceTables = autoReplicateReferenceTables;
  }

  public Boolean getConnectToPooler() {
    return connectToPooler;
  }

  public void setConnectToPooler(Boolean connectToPooler) {
    this.connectToPooler = connectToPooler;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

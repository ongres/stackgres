/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedcluster;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ShardedClusterSpecLabels {

  private Map<String, String> coordinatorPrimaryService;

  private Map<String, String> coordinatorAnyService;

  private Map<String, String> shardsPrimariesService;

  public Map<String, String> getCoordinatorPrimaryService() {
    return coordinatorPrimaryService;
  }

  public void setCoordinatorPrimaryService(Map<String, String> coordinatorPrimaryService) {
    this.coordinatorPrimaryService = coordinatorPrimaryService;
  }

  public Map<String, String> getCoordinatorAnyService() {
    return coordinatorAnyService;
  }

  public void setCoordinatorAnyService(Map<String, String> coordinatorAnyService) {
    this.coordinatorAnyService = coordinatorAnyService;
  }

  public Map<String, String> getShardsPrimariesService() {
    return shardsPrimariesService;
  }

  public void setShardsPrimariesService(Map<String, String> shardsPrimariesService) {
    this.shardsPrimariesService = shardsPrimariesService;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

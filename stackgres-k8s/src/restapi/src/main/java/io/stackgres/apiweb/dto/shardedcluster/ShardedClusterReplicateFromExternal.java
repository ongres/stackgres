/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedcluster;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.cluster.ClusterReplicateFromCustomRestoreMethod;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ShardedClusterReplicateFromExternal {

  private List<String> hosts;

  private List<Integer> ports;

  private List<ClusterReplicateFromCustomRestoreMethod> customRestoreMethods;

  public List<String> getHosts() {
    return hosts;
  }

  public void setHosts(List<String> hosts) {
    this.hosts = hosts;
  }

  public List<Integer> getPorts() {
    return ports;
  }

  public void setPorts(List<Integer> ports) {
    this.ports = ports;
  }

  public List<ClusterReplicateFromCustomRestoreMethod> getCustomRestoreMethods() {
    return customRestoreMethods;
  }

  public void setCustomRestoreMethods(
      List<ClusterReplicateFromCustomRestoreMethod> customRestoreMethods) {
    this.customRestoreMethods = customRestoreMethods;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

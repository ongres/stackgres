/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedcluster;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.cluster.ClusterCredentials;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ShardedClusterConfigurations {

  private List<ShardedClusterBackupConfiguration> backups;

  private ClusterCredentials credentials;

  public List<ShardedClusterBackupConfiguration> getBackups() {
    return backups;
  }

  public void setBackups(List<ShardedClusterBackupConfiguration> backups) {
    this.backups = backups;
  }

  public ClusterCredentials getCredentials() {
    return credentials;
  }

  public void setCredentials(ClusterCredentials credentials) {
    this.credentials = credentials;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

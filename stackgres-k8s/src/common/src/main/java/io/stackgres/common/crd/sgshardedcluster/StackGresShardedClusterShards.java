/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import java.util.Objects;

import javax.validation.constraints.Positive;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresShardedClusterShards extends StackGresClusterSpec {

  @JsonProperty("clusters")
  @Positive(message = "You need at least 1 cluster in the shards")
  private int clusters;

  @JsonProperty("instancesPerCluster")
  @Positive(message = "You need at least 1 instance in each cluster")
  private int instancesPerCluster;

  @Override
  public boolean isPosgresSectionPresent() {
    return true;
  }

  @Override
  public boolean isInstancesPositive() {
    return true;
  }

  @Override
  public boolean isReplicationSectionPresent() {
    return true;
  }

  @Override
  public boolean isSupportingInstancesForInstancesInReplicationGroups() {
    return true;
  }

  @Override
  public boolean isSupportingRequiredSynchronousReplicas() {
    return true;
  }

  public int getClusters() {
    return clusters;
  }

  public void setClusters(int clusters) {
    this.clusters = clusters;
  }

  public int getInstancesPerCluster() {
    return instancesPerCluster;
  }

  public void setInstancesPerCluster(int instancesPerCluster) {
    this.instancesPerCluster = instancesPerCluster;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(clusters, instancesPerCluster);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (!(obj instanceof StackGresShardedClusterShards)) {
      return false;
    }
    StackGresShardedClusterShards other = (StackGresShardedClusterShards) obj;
    return clusters == other.clusters && instancesPerCluster == other.instancesPerCluster;
  }

}

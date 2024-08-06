/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedcluster;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShardedClusterShardingSphereRepository {

  private String type;

  private Map<String, String> properties;

  private ShardedClusterShardingSphereZooKeeper zooKeeper;

  private ShardedClusterShardingSphereEtcd etcd;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public ShardedClusterShardingSphereZooKeeper getZooKeeper() {
    return zooKeeper;
  }

  public void setZooKeeper(ShardedClusterShardingSphereZooKeeper zooKeeper) {
    this.zooKeeper = zooKeeper;
  }

  public ShardedClusterShardingSphereEtcd getEtcd() {
    return etcd;
  }

  public void setEtcd(ShardedClusterShardingSphereEtcd etcd) {
    this.etcd = etcd;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

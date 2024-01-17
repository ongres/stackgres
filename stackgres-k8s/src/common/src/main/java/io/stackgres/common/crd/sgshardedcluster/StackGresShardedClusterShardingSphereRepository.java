/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.ValidEnum;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresShardedClusterShardingSphereRepository {

  @ValidEnum(enumClass = StackGresShardingSphereRepositoryType.class, allowNulls = false,
      message = "supported type are memory, zooKeeper and etcd")
  private String type;

  private Map<String, String> properties;

  private StackGresShardedClusterShardingSphereZooKeeper zooKeeper;

  private StackGresShardedClusterShardingSphereEtcd etcd;

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

  public StackGresShardedClusterShardingSphereZooKeeper getZooKeeper() {
    return zooKeeper;
  }

  public void setZooKeeper(StackGresShardedClusterShardingSphereZooKeeper zooKeeper) {
    this.zooKeeper = zooKeeper;
  }

  public StackGresShardedClusterShardingSphereEtcd getEtcd() {
    return etcd;
  }

  public void setEtcd(StackGresShardedClusterShardingSphereEtcd etcd) {
    this.etcd = etcd;
  }

  @Override
  public int hashCode() {
    return Objects.hash(etcd, properties, type, zooKeeper);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresShardedClusterShardingSphereRepository)) {
      return false;
    }
    StackGresShardedClusterShardingSphereRepository other = (StackGresShardedClusterShardingSphereRepository) obj;
    return Objects.equals(etcd, other.etcd) && Objects.equals(properties, other.properties)
        && Objects.equals(type, other.type) && Objects.equals(zooKeeper, other.zooKeeper);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

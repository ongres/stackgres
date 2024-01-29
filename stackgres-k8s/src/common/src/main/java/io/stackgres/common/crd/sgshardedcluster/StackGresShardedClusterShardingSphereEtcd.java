/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresShardedClusterShardingSphereEtcd {

  private List<String> serverList;

  private Integer timeToLiveSeconds;

  private Integer connectionTimeout;

  public List<String> getServerList() {
    return serverList;
  }

  public void setServerList(List<String> serverList) {
    this.serverList = serverList;
  }

  public Integer getTimeToLiveSeconds() {
    return timeToLiveSeconds;
  }

  public void setTimeToLiveSeconds(Integer timeToLiveSeconds) {
    this.timeToLiveSeconds = timeToLiveSeconds;
  }

  public Integer getConnectionTimeout() {
    return connectionTimeout;
  }

  public void setConnectionTimeout(Integer connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  @Override
  public int hashCode() {
    return Objects.hash(connectionTimeout, serverList, timeToLiveSeconds);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresShardedClusterShardingSphereEtcd)) {
      return false;
    }
    StackGresShardedClusterShardingSphereEtcd other = (StackGresShardedClusterShardingSphereEtcd) obj;
    return Objects.equals(connectionTimeout, other.connectionTimeout) && Objects.equals(serverList, other.serverList)
        && Objects.equals(timeToLiveSeconds, other.timeToLiveSeconds);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

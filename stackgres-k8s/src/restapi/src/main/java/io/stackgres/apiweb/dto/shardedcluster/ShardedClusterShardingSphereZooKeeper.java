/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedcluster;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShardedClusterShardingSphereZooKeeper {

  private List<String> serverList;

  private Integer retryIntervalMilliseconds;

  private Integer maxRetries;

  private Integer timeToLiveSeconds;

  private Integer operationTimeoutMilliseconds;

  private String digest;

  public List<String> getServerList() {
    return serverList;
  }

  public void setServerList(List<String> serverList) {
    this.serverList = serverList;
  }

  public Integer getRetryIntervalMilliseconds() {
    return retryIntervalMilliseconds;
  }

  public void setRetryIntervalMilliseconds(Integer retryIntervalMilliseconds) {
    this.retryIntervalMilliseconds = retryIntervalMilliseconds;
  }

  public Integer getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(Integer maxRetries) {
    this.maxRetries = maxRetries;
  }

  public Integer getTimeToLiveSeconds() {
    return timeToLiveSeconds;
  }

  public void setTimeToLiveSeconds(Integer timeToLiveSeconds) {
    this.timeToLiveSeconds = timeToLiveSeconds;
  }

  public Integer getOperationTimeoutMilliseconds() {
    return operationTimeoutMilliseconds;
  }

  public void setOperationTimeoutMilliseconds(Integer operationTimeoutMilliseconds) {
    this.operationTimeoutMilliseconds = operationTimeoutMilliseconds;
  }

  public String getDigest() {
    return digest;
  }

  public void setDigest(String digest) {
    this.digest = digest;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

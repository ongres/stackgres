/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedbackup;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.AdditionalProperties;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ShardedBackupSpec extends AdditionalProperties {

  private String sgShardedCluster;

  private Boolean managedLifecycle;

  private Integer timeout;

  private Integer reconciliationTimeout;

  private Integer maxRetries;

  private String retryDelay;

  private Integer retryLimit;

  private String retryMaxDelay;

  public String getSgShardedCluster() {
    return sgShardedCluster;
  }

  public void setSgShardedCluster(String sgShardedCluster) {
    this.sgShardedCluster = sgShardedCluster;
  }

  public Boolean getManagedLifecycle() {
    return managedLifecycle;
  }

  public void setManagedLifecycle(Boolean managedLifecycle) {
    this.managedLifecycle = managedLifecycle;
  }

  public Integer getTimeout() {
    return timeout;
  }

  public void setTimeout(Integer timeout) {
    this.timeout = timeout;
  }

  public Integer getReconciliationTimeout() {
    return reconciliationTimeout;
  }

  public void setReconciliationTimeout(Integer reconciliationTimeout) {
    this.reconciliationTimeout = reconciliationTimeout;
  }

  public Integer getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(Integer maxRetries) {
    this.maxRetries = maxRetries;
  }

  public String getRetryDelay() {
    return retryDelay;
  }

  public void setRetryDelay(String retryDelay) {
    this.retryDelay = retryDelay;
  }

  public Integer getRetryLimit() {
    return retryLimit;
  }

  public void setRetryLimit(Integer retryLimit) {
    this.retryLimit = retryLimit;
  }

  public String getRetryMaxDelay() {
    return retryMaxDelay;
  }

  public void setRetryMaxDelay(String retryMaxDelay) {
    this.retryMaxDelay = retryMaxDelay;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardeddbops;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ShardedDbOpsSpec {

  private String sgShardedCluster;

  private ShardedDbOpsSpecScheduling scheduling;

  private String op;

  private String runAt;

  private String timeout;

  private Integer maxRetries;

  private ShardedDbOpsResharding resharding;

  private ShardedDbOpsMajorVersionUpgrade majorVersionUpgrade;

  private ShardedDbOpsRestart restart;

  private ShardedDbOpsMinorVersionUpgrade minorVersionUpgrade;

  private ShardedDbOpsSecurityUpgrade securityUpgrade;

  public String getSgShardedCluster() {
    return sgShardedCluster;
  }

  public void setSgShardedCluster(String sgShardedCluster) {
    this.sgShardedCluster = sgShardedCluster;
  }

  public ShardedDbOpsSpecScheduling getScheduling() {
    return scheduling;
  }

  public void setScheduling(ShardedDbOpsSpecScheduling scheduling) {
    this.scheduling = scheduling;
  }

  public String getOp() {
    return op;
  }

  public void setOp(String op) {
    this.op = op;
  }

  public String getRunAt() {
    return runAt;
  }

  public void setRunAt(String runAt) {
    this.runAt = runAt;
  }

  public String getTimeout() {
    return timeout;
  }

  public void setTimeout(String timeout) {
    this.timeout = timeout;
  }

  public Integer getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(Integer maxRetries) {
    this.maxRetries = maxRetries;
  }

  public ShardedDbOpsResharding getResharding() {
    return resharding;
  }

  public void setResharding(ShardedDbOpsResharding resharding) {
    this.resharding = resharding;
  }

  public ShardedDbOpsMajorVersionUpgrade getMajorVersionUpgrade() {
    return majorVersionUpgrade;
  }

  public void setMajorVersionUpgrade(ShardedDbOpsMajorVersionUpgrade majorVersionUpgrade) {
    this.majorVersionUpgrade = majorVersionUpgrade;
  }

  public ShardedDbOpsRestart getRestart() {
    return restart;
  }

  public void setRestart(ShardedDbOpsRestart restart) {
    this.restart = restart;
  }

  public ShardedDbOpsMinorVersionUpgrade getMinorVersionUpgrade() {
    return minorVersionUpgrade;
  }

  public void setMinorVersionUpgrade(ShardedDbOpsMinorVersionUpgrade minorVersionUpgrade) {
    this.minorVersionUpgrade = minorVersionUpgrade;
  }

  public ShardedDbOpsSecurityUpgrade getSecurityUpgrade() {
    return securityUpgrade;
  }

  public void setSecurityUpgrade(ShardedDbOpsSecurityUpgrade securityUpgrade) {
    this.securityUpgrade = securityUpgrade;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

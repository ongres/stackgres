/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardeddbops;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsMajorVersionUpgrade;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsMinorVersionUpgrade;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsResharding;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsRestart;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsSecurityUpgrade;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsSpecScheduling;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ShardedDbOpsSpec {

  private String sgShardedCluster;

  private StackGresShardedDbOpsSpecScheduling scheduling;

  private String op;

  private String runAt;

  private String timeout;

  private Integer maxRetries;

  private StackGresShardedDbOpsResharding resharding;

  private StackGresShardedDbOpsMajorVersionUpgrade majorVersionUpgrade;

  private StackGresShardedDbOpsRestart restart;

  private StackGresShardedDbOpsMinorVersionUpgrade minorVersionUpgrade;

  private StackGresShardedDbOpsSecurityUpgrade securityUpgrade;

  public String getSgShardedCluster() {
    return sgShardedCluster;
  }

  public void setSgShardedCluster(String sgShardedCluster) {
    this.sgShardedCluster = sgShardedCluster;
  }

  public StackGresShardedDbOpsSpecScheduling getScheduling() {
    return scheduling;
  }

  public void setScheduling(StackGresShardedDbOpsSpecScheduling scheduling) {
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

  public StackGresShardedDbOpsResharding getResharding() {
    return resharding;
  }

  public void setResharding(StackGresShardedDbOpsResharding resharding) {
    this.resharding = resharding;
  }

  public StackGresShardedDbOpsMajorVersionUpgrade getMajorVersionUpgrade() {
    return majorVersionUpgrade;
  }

  public void setMajorVersionUpgrade(StackGresShardedDbOpsMajorVersionUpgrade majorVersionUpgrade) {
    this.majorVersionUpgrade = majorVersionUpgrade;
  }

  public StackGresShardedDbOpsRestart getRestart() {
    return restart;
  }

  public void setRestart(StackGresShardedDbOpsRestart restart) {
    this.restart = restart;
  }

  public StackGresShardedDbOpsMinorVersionUpgrade getMinorVersionUpgrade() {
    return minorVersionUpgrade;
  }

  public void setMinorVersionUpgrade(StackGresShardedDbOpsMinorVersionUpgrade minorVersionUpgrade) {
    this.minorVersionUpgrade = minorVersionUpgrade;
  }

  public StackGresShardedDbOpsSecurityUpgrade getSecurityUpgrade() {
    return securityUpgrade;
  }

  public void setSecurityUpgrade(StackGresShardedDbOpsSecurityUpgrade securityUpgrade) {
    this.securityUpgrade = securityUpgrade;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.dbops;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DbOpsStatus {

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private List<DbOpsCondition> conditions = new ArrayList<>();

  private Integer opRetries;

  private String opStarted;

  private DbOpsBenchmarkStatus benchmark;

  private DbOpsMajorVersionUpgradeStatus majorVersionUpgrade;

  private DbOpsRestartStatus restart;

  private DbOpsMinorVersionUpgradeStatus minorVersionUpgrade;

  private DbOpsSecurityUpgradeStatus securityUpgrade;

  public List<DbOpsCondition> getConditions() {
    return conditions;
  }

  public void setConditions(List<DbOpsCondition> conditions) {
    this.conditions = conditions;
  }

  public Integer getOpRetries() {
    return opRetries;
  }

  public void setOpRetries(Integer opRetries) {
    this.opRetries = opRetries;
  }

  public String getOpStarted() {
    return opStarted;
  }

  public void setOpStarted(String opStarted) {
    this.opStarted = opStarted;
  }

  public DbOpsBenchmarkStatus getBenchmark() {
    return benchmark;
  }

  public void setBenchmark(DbOpsBenchmarkStatus benchmark) {
    this.benchmark = benchmark;
  }

  public DbOpsMajorVersionUpgradeStatus getMajorVersionUpgrade() {
    return majorVersionUpgrade;
  }

  public void setMajorVersionUpgrade(DbOpsMajorVersionUpgradeStatus majorVersionUpgrade) {
    this.majorVersionUpgrade = majorVersionUpgrade;
  }

  public DbOpsRestartStatus getRestart() {
    return restart;
  }

  public void setRestart(DbOpsRestartStatus restart) {
    this.restart = restart;
  }

  public DbOpsMinorVersionUpgradeStatus getMinorVersionUpgrade() {
    return minorVersionUpgrade;
  }

  public void setMinorVersionUpgrade(DbOpsMinorVersionUpgradeStatus minorVersionUpgrade) {
    this.minorVersionUpgrade = minorVersionUpgrade;
  }

  public DbOpsSecurityUpgradeStatus getSecurityUpgrade() {
    return securityUpgrade;
  }

  public void setSecurityUpgrade(DbOpsSecurityUpgradeStatus securityUpgrade) {
    this.securityUpgrade = securityUpgrade;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

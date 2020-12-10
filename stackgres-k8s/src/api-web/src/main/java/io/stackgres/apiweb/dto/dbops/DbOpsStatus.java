/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.dbops;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class DbOpsStatus {

  @JsonProperty("conditions")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private List<DbOpsCondition> conditions = new ArrayList<>();

  @JsonProperty("opRetries")
  private Integer opRetries;

  @JsonProperty("opStarted")
  private String opStarted;

  @JsonProperty("benchmark")
  private DbOpsBenchmarkStatus benchmark;

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

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

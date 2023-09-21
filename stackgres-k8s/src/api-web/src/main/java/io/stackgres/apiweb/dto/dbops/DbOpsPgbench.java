/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.dbops;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DbOpsPgbench {

  private String databaseSize;

  private String duration;

  private Boolean usePreparedStatements;

  private Integer concurrentClients;

  private Integer threads;

  public String getDatabaseSize() {
    return databaseSize;
  }

  public void setDatabaseSize(String databaseSize) {
    this.databaseSize = databaseSize;
  }

  public String getDuration() {
    return duration;
  }

  public void setDuration(String duration) {
    this.duration = duration;
  }

  public Boolean getUsePreparedStatements() {
    return usePreparedStatements;
  }

  public void setUsePreparedStatements(Boolean usePreparedStatements) {
    this.usePreparedStatements = usePreparedStatements;
  }

  public Integer getConcurrentClients() {
    return concurrentClients;
  }

  public void setConcurrentClients(Integer concurrentClients) {
    this.concurrentClients = concurrentClients;
  }

  public Integer getThreads() {
    return threads;
  }

  public void setThreads(Integer threads) {
    this.threads = threads;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.dbops;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DbOpsPgbench {

  private String mode;

  private String databaseSize;

  private String duration;

  private Boolean usePreparedStatements;

  private String queryMode;

  private Integer concurrentClients;

  private Integer threads;

  private BigDecimal samplingRate;

  private Boolean foreignKeys;

  private Boolean unloggedTables;

  private String partitionMethod;

  private Integer partitions;

  private String initSteps;

  private Integer fillfactor;

  private Boolean noVacuum;

  private DbOpsPgbenchCustom custom;

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

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

  public String getQueryMode() {
    return queryMode;
  }

  public void setQueryMode(String queryMode) {
    this.queryMode = queryMode;
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

  public BigDecimal getSamplingRate() {
    return samplingRate;
  }

  public void setSamplingRate(BigDecimal samplingRate) {
    this.samplingRate = samplingRate;
  }

  public Boolean getForeignKeys() {
    return foreignKeys;
  }

  public void setForeignKeys(Boolean foreignKeys) {
    this.foreignKeys = foreignKeys;
  }

  public Boolean getUnloggedTables() {
    return unloggedTables;
  }

  public void setUnloggedTables(Boolean unloggedTables) {
    this.unloggedTables = unloggedTables;
  }

  public String getPartitionMethod() {
    return partitionMethod;
  }

  public void setPartitionMethod(String partitionMethod) {
    this.partitionMethod = partitionMethod;
  }

  public Integer getPartitions() {
    return partitions;
  }

  public void setPartitions(Integer partitions) {
    this.partitions = partitions;
  }

  public String getInitSteps() {
    return initSteps;
  }

  public void setInitSteps(String initSteps) {
    this.initSteps = initSteps;
  }

  public Integer getFillfactor() {
    return fillfactor;
  }

  public void setFillfactor(Integer fillfactor) {
    this.fillfactor = fillfactor;
  }

  public Boolean getNoVacuum() {
    return noVacuum;
  }

  public void setNoVacuum(Boolean noVacuum) {
    this.noVacuum = noVacuum;
  }

  public DbOpsPgbenchCustom getCustom() {
    return custom;
  }

  public void setCustom(DbOpsPgbenchCustom custom) {
    this.custom = custom;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

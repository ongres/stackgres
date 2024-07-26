/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.math.BigDecimal;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.stackgres.common.validation.ValidEnum;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresDbOpsPgbench {

  @ValidEnum(enumClass = DbOpsPgbenchMode.class, allowNulls = true,
      message = "mode must be one of tpcb-like, select-only or custom")
  private String mode;

  @NotEmpty(message = "databaseSize must be provided")
  private String databaseSize;

  @NotEmpty(message = "duration must be provided")
  private String duration;

  private Boolean usePreparedStatements;

  @ValidEnum(enumClass = DbOpsPgbenchQueryMode.class, allowNulls = true,
      message = "queryMode must be one of simple, extended or prepared")
  private String queryMode;

  @Min(value = 1, message = "concurrentClients must be greather or equals to 1.")
  private Integer concurrentClients;

  @Min(value = 1, message = "threads must be greather or equals to 1.")
  private Integer threads;

  private BigDecimal samplingRate;

  private Boolean foreignKeys;

  private Boolean unloggedTables;

  @ValidEnum(enumClass = DbOpsPgbenchPartitionMethod.class, allowNulls = true,
      message = "partitionMethod must be one of range or hash")
  private String partitionMethod;

  private Integer partitions;

  private String initSteps;

  private Integer fillfactor;

  private Boolean noVacuum;

  @Valid
  private StackGresDbOpsPgbenchCustom custom;

  @ReferencedField("duration")
  interface Duration extends FieldReference { }

  @JsonIgnore
  @AssertTrue(message = "duration must be positive and in ISO 8601 duration format:"
      + " `PnDTnHnMn.nS`.",
      payload = Duration.class)
  public boolean isDurationValid() {
    try {
      if (duration != null) {
        return !java.time.Duration.parse(duration).isNegative();
      }
      return true;
    } catch (DateTimeParseException ex) {
      return false;
    }
  }

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

  public StackGresDbOpsPgbenchCustom getCustom() {
    return custom;
  }

  public void setCustom(StackGresDbOpsPgbenchCustom custom) {
    this.custom = custom;
  }

  @Override
  public int hashCode() {
    return Objects.hash(concurrentClients, custom, databaseSize, duration, fillfactor, foreignKeys,
        initSteps, mode, noVacuum, partitionMethod, partitions, queryMode, samplingRate, threads,
        unloggedTables, usePreparedStatements);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDbOpsPgbench)) {
      return false;
    }
    StackGresDbOpsPgbench other = (StackGresDbOpsPgbench) obj;
    return Objects.equals(concurrentClients, other.concurrentClients)
        && Objects.equals(custom, other.custom) && Objects.equals(databaseSize, other.databaseSize)
        && Objects.equals(duration, other.duration) && Objects.equals(fillfactor, other.fillfactor)
        && Objects.equals(foreignKeys, other.foreignKeys)
        && Objects.equals(initSteps, other.initSteps) && Objects.equals(mode, other.mode)
        && Objects.equals(noVacuum, other.noVacuum)
        && Objects.equals(partitionMethod, other.partitionMethod)
        && Objects.equals(partitions, other.partitions)
        && Objects.equals(queryMode, other.queryMode)
        && Objects.equals(samplingRate, other.samplingRate)
        && Objects.equals(threads, other.threads)
        && Objects.equals(unloggedTables, other.unloggedTables)
        && Objects.equals(usePreparedStatements, other.usePreparedStatements);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

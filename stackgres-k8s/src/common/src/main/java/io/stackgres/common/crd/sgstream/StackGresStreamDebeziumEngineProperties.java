/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgstream;

import java.util.Map;
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
public class StackGresStreamDebeziumEngineProperties {

  private String offsetCommitPolicy;

  private Integer offsetFlushIntervalMs;

  private Integer offsetFlushTimeoutMs;

  private Integer errorsMaxRetries;

  private Integer errorsRetryDelayInitialMs;

  private Integer errorsRetryDelayMaxMs;

  @DebeziumMapOptions(generateSummary = true)
  private Map<String, Map<String, String>> transforms;

  @DebeziumMapOptions(generateSummary = true)
  private Map<String, Map<String, String>> predicates;

  private Integer recordProcessingThreads;

  private Integer recordProcessingShutdownTimeoutMs;

  private String recordProcessingOrder;

  private Boolean recordProcessingWithSerialConsumer;

  private Integer taskManagementTimeoutMs;

  public String getOffsetCommitPolicy() {
    return offsetCommitPolicy;
  }

  public void setOffsetCommitPolicy(String offsetCommitPolicy) {
    this.offsetCommitPolicy = offsetCommitPolicy;
  }

  public Integer getOffsetFlushIntervalMs() {
    return offsetFlushIntervalMs;
  }

  public void setOffsetFlushIntervalMs(Integer offsetFlushIntervalMs) {
    this.offsetFlushIntervalMs = offsetFlushIntervalMs;
  }

  public Integer getOffsetFlushTimeoutMs() {
    return offsetFlushTimeoutMs;
  }

  public void setOffsetFlushTimeoutMs(Integer offsetFlushTimeoutMs) {
    this.offsetFlushTimeoutMs = offsetFlushTimeoutMs;
  }

  public Integer getErrorsMaxRetries() {
    return errorsMaxRetries;
  }

  public void setErrorsMaxRetries(Integer errorsMaxRetries) {
    this.errorsMaxRetries = errorsMaxRetries;
  }

  public Integer getErrorsRetryDelayInitialMs() {
    return errorsRetryDelayInitialMs;
  }

  public void setErrorsRetryDelayInitialMs(Integer errorsRetryDelayInitialMs) {
    this.errorsRetryDelayInitialMs = errorsRetryDelayInitialMs;
  }

  public Integer getErrorsRetryDelayMaxMs() {
    return errorsRetryDelayMaxMs;
  }

  public void setErrorsRetryDelayMaxMs(Integer errorsRetryDelayMaxMs) {
    this.errorsRetryDelayMaxMs = errorsRetryDelayMaxMs;
  }

  public Map<String, Map<String, String>> getTransforms() {
    return transforms;
  }

  public void setTransforms(Map<String, Map<String, String>> transforms) {
    this.transforms = transforms;
  }

  public Map<String, Map<String, String>> getPredicates() {
    return predicates;
  }

  public void setPredicates(Map<String, Map<String, String>> predicates) {
    this.predicates = predicates;
  }

  public Integer getRecordProcessingThreads() {
    return recordProcessingThreads;
  }

  public void setRecordProcessingThreads(Integer recordProcessingThreads) {
    this.recordProcessingThreads = recordProcessingThreads;
  }

  public Integer getRecordProcessingShutdownTimeoutMs() {
    return recordProcessingShutdownTimeoutMs;
  }

  public void setRecordProcessingShutdownTimeoutMs(Integer recordProcessingShutdownTimeoutMs) {
    this.recordProcessingShutdownTimeoutMs = recordProcessingShutdownTimeoutMs;
  }

  public String getRecordProcessingOrder() {
    return recordProcessingOrder;
  }

  public void setRecordProcessingOrder(String recordProcessingOrder) {
    this.recordProcessingOrder = recordProcessingOrder;
  }

  public Boolean getRecordProcessingWithSerialConsumer() {
    return recordProcessingWithSerialConsumer;
  }

  public void setRecordProcessingWithSerialConsumer(Boolean recordProcessingWithSerialConsumer) {
    this.recordProcessingWithSerialConsumer = recordProcessingWithSerialConsumer;
  }

  public Integer getTaskManagementTimeoutMs() {
    return taskManagementTimeoutMs;
  }

  public void setTaskManagementTimeoutMs(Integer taskManagementTimeoutMs) {
    this.taskManagementTimeoutMs = taskManagementTimeoutMs;
  }

  @Override
  public int hashCode() {
    return Objects.hash(errorsMaxRetries, errorsRetryDelayInitialMs, errorsRetryDelayMaxMs,
        offsetCommitPolicy, offsetFlushIntervalMs, offsetFlushTimeoutMs, predicates,
        recordProcessingOrder, recordProcessingShutdownTimeoutMs, recordProcessingThreads,
        recordProcessingWithSerialConsumer, taskManagementTimeoutMs, transforms);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresStreamDebeziumEngineProperties)) {
      return false;
    }
    StackGresStreamDebeziumEngineProperties other = (StackGresStreamDebeziumEngineProperties) obj;
    return Objects.equals(errorsMaxRetries, other.errorsMaxRetries)
        && Objects.equals(errorsRetryDelayInitialMs, other.errorsRetryDelayInitialMs)
        && Objects.equals(errorsRetryDelayMaxMs, other.errorsRetryDelayMaxMs)
        && Objects.equals(offsetCommitPolicy, other.offsetCommitPolicy)
        && Objects.equals(offsetFlushIntervalMs, other.offsetFlushIntervalMs)
        && Objects.equals(offsetFlushTimeoutMs, other.offsetFlushTimeoutMs)
        && Objects.equals(predicates, other.predicates)
        && Objects.equals(recordProcessingOrder, other.recordProcessingOrder)
        && Objects.equals(recordProcessingShutdownTimeoutMs,
            other.recordProcessingShutdownTimeoutMs)
        && Objects.equals(recordProcessingThreads, other.recordProcessingThreads)
        && Objects.equals(recordProcessingWithSerialConsumer,
            other.recordProcessingWithSerialConsumer)
        && Objects.equals(taskManagementTimeoutMs, other.taskManagementTimeoutMs)
        && Objects.equals(transforms, other.transforms);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

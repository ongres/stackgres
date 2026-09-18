/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgbackup;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.AdditionalProperties;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresBackupSpec extends AdditionalProperties {

  @NotNull(message = "The cluster name is required")
  private String sgCluster;

  private Boolean managedLifecycle;

  private Integer timeout;

  private Integer reconciliationTimeout;

  @Min(value = 0, message = "maxRetries must be greather or equals to 0.")
  private Integer maxRetries;

  private String retryDelay;

  @Min(value = 0, message = "retryLimit must be greather or equals to 0.")
  private Integer retryLimit;

  private String retryMaxDelay;

  @ReferencedField("retryDelay")
  interface RetryDelay extends FieldReference {
  }

  @ReferencedField("retryMaxDelay")
  interface RetryMaxDelay extends FieldReference {
  }

  @JsonIgnore
  @AssertTrue(message = "retryDelay must be positive and in ISO 8601 duration format:"
      + " `PnDTnHnMn.nS`.",
      payload = RetryDelay.class)
  public boolean isRetryDelayValid() {
    try {
      if (retryDelay != null) {
        return !Duration.parse(retryDelay).isNegative();
      }
      return true;
    } catch (DateTimeParseException ex) {
      return false;
    }
  }

  @JsonIgnore
  @AssertTrue(message = "retryMaxDelay must be positive and in ISO 8601 duration format:"
      + " `PnDTnHnMn.nS`.",
      payload = RetryMaxDelay.class)
  public boolean isRetryMaxDelayValid() {
    try {
      if (retryMaxDelay != null) {
        return !Duration.parse(retryMaxDelay).isNegative();
      }
      return true;
    } catch (DateTimeParseException ex) {
      return false;
    }
  }

  public String getSgCluster() {
    return sgCluster;
  }

  public void setSgCluster(String sgCluster) {
    this.sgCluster = sgCluster;
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
  public int hashCode() {
    return Objects.hash(managedLifecycle, maxRetries, reconciliationTimeout, retryDelay, retryLimit,
        retryMaxDelay, sgCluster, timeout);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresBackupSpec)) {
      return false;
    }
    StackGresBackupSpec other = (StackGresBackupSpec) obj;
    return Objects.equals(managedLifecycle, other.managedLifecycle)
        && Objects.equals(maxRetries, other.maxRetries)
        && Objects.equals(reconciliationTimeout, other.reconciliationTimeout)
        && Objects.equals(retryDelay, other.retryDelay)
        && Objects.equals(retryLimit, other.retryLimit)
        && Objects.equals(retryMaxDelay, other.retryMaxDelay)
        && Objects.equals(sgCluster, other.sgCluster) && Objects.equals(timeout, other.timeout);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

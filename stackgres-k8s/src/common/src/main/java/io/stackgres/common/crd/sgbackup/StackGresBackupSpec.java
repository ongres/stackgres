/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgbackup;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresBackupSpec {

  @NotNull(message = "The cluster name is required")
  private String sgCluster;

  private Boolean managedLifecycle;

  private Integer timeout;

  private Integer reconciliationTimeout;

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

  @Override
  public int hashCode() {
    return Objects.hash(managedLifecycle, reconciliationTimeout, sgCluster, timeout);
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
        && Objects.equals(reconciliationTimeout, other.reconciliationTimeout)
        && Objects.equals(sgCluster, other.sgCluster) && Objects.equals(timeout, other.timeout);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

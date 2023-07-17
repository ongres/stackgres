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
import io.stackgres.common.validation.ValidEnum;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresBackupProcess {

  @ValidEnum(enumClass = BackupStatus.class, allowNulls = true,
      message = "status must be one of Pending, Backoff, Running, Completed or Failed")
  private String status;
  private String jobPod;
  private String failure;
  private Boolean managedLifecycle;

  @Valid
  private StackGresBackupTiming timing;

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getJobPod() {
    return jobPod;
  }

  public void setJobPod(String jobPod) {
    this.jobPod = jobPod;
  }

  public String getFailure() {
    return failure;
  }

  public void setFailure(String failure) {
    this.failure = failure;
  }

  public StackGresBackupTiming getTiming() {
    return timing;
  }

  public void setTiming(StackGresBackupTiming timing) {
    this.timing = timing;
  }

  public Boolean getManagedLifecycle() {
    return managedLifecycle;
  }

  public void setManagedLifecycle(Boolean managedLifecycle) {
    this.managedLifecycle = managedLifecycle;
  }

  @Override
  public int hashCode() {
    return Objects.hash(failure, jobPod, managedLifecycle, status, timing);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresBackupProcess)) {
      return false;
    }
    StackGresBackupProcess other = (StackGresBackupProcess) obj;
    return Objects.equals(failure, other.failure)
        && Objects.equals(jobPod, other.jobPod)
        && Objects.equals(managedLifecycle, other.managedLifecycle)
        && Objects.equals(status, other.status)
        && Objects.equals(timing, other.timing);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgbackup;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresBackupProcess {

  private String status;
  private String jobPod;
  private String failure;
  private Boolean managedLifecycle;

  private StackgresBackupTiming timing;

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

  public StackgresBackupTiming getTiming() {
    return timing;
  }

  public void setTiming(StackgresBackupTiming timing) {
    this.timing = timing;
  }

  public Boolean getManagedLifecycle() {
    return managedLifecycle;
  }

  public void setManagedLifecycle(Boolean managedLifecycle) {
    this.managedLifecycle = managedLifecycle;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("status", status)
        .add("jobPod", jobPod)
        .add("failure", failure)
        .add("subjectToRetentionPolicy", managedLifecycle)
        .add("timing", timing)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StackGresBackupProcess that = (StackGresBackupProcess) o;
    return Objects.equals(status, that.status)
        && Objects.equals(jobPod, that.jobPod)
        && Objects.equals(failure, that.failure)
        && Objects.equals(managedLifecycle, that.managedLifecycle)
        && Objects.equals(timing, that.timing);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, jobPod, failure, managedLifecycle, timing);
  }
}

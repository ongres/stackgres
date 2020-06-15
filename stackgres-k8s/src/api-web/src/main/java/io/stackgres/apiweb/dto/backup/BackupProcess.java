/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.backup;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class BackupProcess {

  private String status;
  private String jobPod;
  private String failure;
  private Boolean managedLifecycle;
  private BackupTiming timing;

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

  public Boolean getManagedLifecycle() {
    return managedLifecycle;
  }

  public void setManagedLifecycle(Boolean managedLifecycle) {
    this.managedLifecycle = managedLifecycle;
  }

  public BackupTiming getTiming() {
    return timing;
  }

  public void setTiming(BackupTiming timing) {
    this.timing = timing;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("status", status)
        .add("jobPod", jobPod)
        .add("failure", failure)
        .add("managedLifecycle", managedLifecycle)
        .add("timing", timing)
        .toString();
  }
}

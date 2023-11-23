/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedbackup;

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
public class StackGresShardedBackupProcess {

  @ValidEnum(enumClass = ShardedBackupStatus.class, allowNulls = true,
      message = "status must be one of Pending, Backoff, Running, Completed or Failed")
  private String status;
  private String jobPod;
  private String failure;

  @Valid
  private StackGresShardedBackupTiming timing;

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

  public StackGresShardedBackupTiming getTiming() {
    return timing;
  }

  public void setTiming(StackGresShardedBackupTiming timing) {
    this.timing = timing;
  }

  @Override
  public int hashCode() {
    return Objects.hash(failure, jobPod, status, timing);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresShardedBackupProcess)) {
      return false;
    }
    StackGresShardedBackupProcess other = (StackGresShardedBackupProcess) obj;
    return Objects.equals(failure, other.failure)
        && Objects.equals(jobPod, other.jobPod)
        && Objects.equals(status, other.status)
        && Objects.equals(timing, other.timing);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

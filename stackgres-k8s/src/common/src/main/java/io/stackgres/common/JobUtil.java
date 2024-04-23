/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobCondition;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;

public interface JobUtil {

  String JOB_NAME_KEY = "job-name";
  String COMPLETE_JOB_CONDITION_TYPES = "Complete";
  String FAILED_JOB_CONDITION_TYPES = "Failed";
  List<String> COMPLETE_AND_FAILED_JOB_CONDITION_TYPES =
      List.of(COMPLETE_JOB_CONDITION_TYPES, FAILED_JOB_CONDITION_TYPES);

  static Optional<Boolean> isJobCompleteOrFailed(Optional<Job> job) {
    return job
        .map(Job::getStatus)
        .map(JobStatus::getConditions)
        .stream()
        .flatMap(List::stream)
        .filter(condition -> COMPLETE_AND_FAILED_JOB_CONDITION_TYPES.contains(condition.getType()))
        .filter(condition -> condition.getStatus().equals("True"))
        .map(JobCondition::getType)
        .map(COMPLETE_JOB_CONDITION_TYPES::equals)
        .findFirst();
  }

  static Optional<Boolean> hasJobFailed(Optional<Job> job) {
    return job
        .map(Job::getStatus)
        .map(JobStatus::getFailed)
        .map(failed -> failed > 0);
  }

  static Optional<Boolean> isJobActive(Optional<Job> job) {
    return job
        .map(Job::getStatus)
        .map(JobStatus::getActive)
        .map(active -> active > 0);
  }

  static Optional<Map<String, String>> getJobPodsMatchLabels(Job job) {
    return Optional.ofNullable(job)
        .map(Job::getSpec)
        .map(JobSpec::getSelector)
        .map(LabelSelector::getMatchLabels);
  }

}

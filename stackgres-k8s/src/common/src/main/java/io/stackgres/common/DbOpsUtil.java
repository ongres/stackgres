/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.DbOpsStatusCondition.Status;
import io.stackgres.common.crd.sgdbops.DbOpsStatusCondition.Type;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.operatorframework.resource.ResourceUtil;

public interface DbOpsUtil {

  List<String> ROLLOUT_OPS = List.of(
      "restart",
      "securityUpgrade",
      "minorVersionUpgrade");

  Pattern UPPERCASE_LETTER_PATTERN = Pattern.compile("([A-Z])");

  String SUFFIX = "-dbops";

  static String roleName(StackGresCluster cluster) {
    return roleName(cluster.getMetadata().getName());
  }

  static String roleName(String clusterName) {
    return ResourceUtil.resourceName(clusterName + SUFFIX);
  }

  static boolean isAlreadyCompleted(StackGresDbOps dbOps) {
    return Optional.of(dbOps)
        .map(StackGresDbOps::getStatus)
        .map(StackGresDbOpsStatus::getConditions)
        .stream()
        .flatMap(List::stream)
        .filter(condition -> Status.TRUE.getStatus().equals(condition.getStatus()))
        .anyMatch(condition -> Type.COMPLETED.getType().equals(condition.getType())
            || Type.FAILED.getType().equals(condition.getType()));
  }

  static boolean isAlreadySuccessfullyCompleted(StackGresDbOps dbOps) {
    return Optional.of(dbOps)
        .map(StackGresDbOps::getStatus)
        .map(StackGresDbOpsStatus::getConditions)
        .stream()
        .flatMap(List::stream)
        .filter(condition -> Status.TRUE.getStatus().equals(condition.getStatus()))
        .anyMatch(condition -> Type.COMPLETED.getType().equals(condition.getType()));
  }

  static String jobName(StackGresDbOps dbOps) {
    return jobName(dbOps, getKebabCaseOperation(dbOps));
  }

  static String jobName(StackGresDbOps dbOps, String operation) {
    String name = dbOps.getMetadata().getName();
    return ResourceUtil.resourceName(name);
  }

  static String getTimeout(StackGresDbOps dbOps) {
    return Optional.of(dbOps)
        .map(StackGresDbOps::getSpec)
        .map(StackGresDbOpsSpec::getTimeout)
        .map(Duration::parse)
        .map(Duration::getSeconds)
        .map(Object::toString)
        .orElseGet(() -> String.valueOf(Integer.MAX_VALUE));
  }

  static String getKebabCaseOperation(StackGresDbOps dbOps) {
    return UPPERCASE_LETTER_PATTERN
        .matcher(dbOps.getSpec().getOp())
        .replaceAll(m -> "-" + m.group().toLowerCase(Locale.US));
  }

  public static Boolean isToRunAfter(StackGresDbOps dbOps, Instant now) {
    return Optional.of(dbOps)
        .map(StackGresDbOps::getSpec)
        .map(StackGresDbOpsSpec::getRunAt)
        .map(Instant::parse)
        .map(runAt -> !runAt.isBefore(now))
        .orElse(false);
  }

}

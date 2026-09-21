/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.DbOpsOperation;
import io.stackgres.common.crd.sgdbops.DbOpsStatusCondition;
import io.stackgres.common.crd.sgdbops.DbOpsStatusCondition.Status;
import io.stackgres.common.crd.sgdbops.DbOpsStatusCondition.Type;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsMinorVersionUpgrade;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsRestart;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSecurityUpgrade;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.slf4j.LoggerFactory;

public interface DbOpsUtil {

  List<String> ROLLOUT_OPS = List.of(
      DbOpsOperation.RESTART.toString(),
      DbOpsOperation.SECURITY_UPGRADE.toString(),
      DbOpsOperation.MINOR_VERSION_UPGRADE.toString());

  Pattern UPPERCASE_LETTER_PATTERN = Pattern.compile("([A-Z])");

  String SUFFIX = "-dbops";

  Duration DEFAULT_STATUS_UPDATE_DELAY = Duration.ofSeconds(10);

  // Phases set on SGDbOps.status.majorVersionUpgrade.phase when manualRollback is enabled and the
  // operation pauses waiting for the SGDbOps.status.majorVersionUpgrade.rollback decision.
  // Must match the values set by the run-major-version-upgrade.sh shell script.
  String MAJOR_VERSION_UPGRADE_WAIT_POST_UPGRADE_DECISION_PHASE = "wait-post-upgrade-decision";
  String MAJOR_VERSION_UPGRADE_WAIT_POST_FAILED_UPGRADE_DECISION_PHASE =
      "wait-post-failed-upgrade-decision";

  static String roleName(StackGresCluster cluster) {
    return roleName(cluster.getMetadata().getName());
  }

  static String roleName(String clusterName) {
    return ResourceUtil.resourceName(clusterName + SUFFIX);
  }

  static boolean isAlreadyRollout(StackGresDbOps dbOps) {
    return Optional.of(dbOps)
        .map(StackGresDbOps::getStatus)
        .map(StackGresDbOpsStatus::getConditions)
        .stream()
        .flatMap(List::stream)
        .filter(condition -> Status.TRUE.getStatus().equals(condition.getStatus()))
        .anyMatch(condition -> Type.ROLLOUT_COMPLETED.getType().equals(condition.getType())
            || Type.FAILED.getType().equals(condition.getType()));
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

  static String getRetryDelay(StackGresDbOps dbOps) {
    return Optional.of(dbOps)
        .map(StackGresDbOps::getSpec)
        .map(StackGresDbOpsSpec::getRetryDelay)
        .map(Duration::parse)
        .map(Duration::toMillis)
        .map(Object::toString)
        .orElse("1000");
  }

  static String getRetryLimit(StackGresDbOps dbOps) {
    return Optional.of(dbOps)
        .map(StackGresDbOps::getSpec)
        .map(StackGresDbOpsSpec::getRetryLimit)
        .map(Object::toString)
        .orElse("10");
  }

  static String getRetryMaxDelay(StackGresDbOps dbOps) {
    return Optional.of(dbOps)
        .map(StackGresDbOps::getSpec)
        .map(StackGresDbOpsSpec::getRetryMaxDelay)
        .map(Duration::parse)
        .map(Duration::toMillis)
        .map(Object::toString)
        .orElse("60000");
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

  /**
   * The delay that has to pass, since the last update of the status of a rollout operation, before
   * such operation can be considered completed.
   */
  static Duration getStatusUpdateDelay(StackGresDbOps dbOps) {
    return Optional.of(dbOps)
        .map(StackGresDbOps::getSpec)
        .flatMap(spec -> Optional.of(spec)
            .map(StackGresDbOpsSpec::getRestart)
            .map(StackGresDbOpsRestart::getStatusUpdateDelay)
            .or(() -> Optional.of(spec)
                .map(StackGresDbOpsSpec::getSecurityUpgrade)
                .map(StackGresDbOpsSecurityUpgrade::getStatusUpdateDelay))
            .or(() -> Optional.of(spec)
                .map(StackGresDbOpsSpec::getMinorVersionUpgrade)
                .map(StackGresDbOpsMinorVersionUpgrade::getStatusUpdateDelay)))
        .map(DbOpsUtil::parseStatusUpdateDelay)
        .orElse(DEFAULT_STATUS_UPDATE_DELAY);
  }

  private static Duration parseStatusUpdateDelay(String statusUpdateDelay) {
    try {
      return Duration.parse(statusUpdateDelay);
    } catch (DateTimeParseException ex) {
      LoggerFactory.getLogger(DbOpsUtil.class)
          .warn("Status update delay {} is not valid, using the default of {}",
              statusUpdateDelay, DEFAULT_STATUS_UPDATE_DELAY, ex);
      return DEFAULT_STATUS_UPDATE_DELAY;
    }
  }

  public static Boolean isTimeoutExpired(StackGresDbOps dbOps, Instant now) {
    return Optional.of(dbOps)
        .map(StackGresDbOps::getSpec)
        .map(StackGresDbOpsSpec::getTimeout)
        .map(Duration::parse)
        .map(timeout -> Optional.of(dbOps)
            .map(StackGresDbOps::getStatus)
            .map(StackGresDbOpsStatus::getConditions)
            .stream()
            .flatMap(List::stream)
            .filter(DbOpsStatusCondition.DBOPS_RUNNING::isCondition)
            .map(Condition::getLastTransitionTime)
            .findFirst()
            .map(Instant::parse)
            .map(started -> !started.plus(timeout).isBefore(now))
                .orElse(false))
        .orElse(false);
  }

}

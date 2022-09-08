/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import com.google.common.base.Predicates;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.DbOpsStatusCondition;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.operatorframework.resource.ResourceUtil;

public interface DbOpsUtil {

  Pattern UPPERCASE_LETTER_PATTERN = Pattern.compile("([A-Z])");

  String SUFFIX = "-dbops";

  static String roleName(StackGresCluster cluster) {
    return roleName(cluster.getMetadata().getName());
  }

  static String roleName(String clusterName) {
    return ResourceUtil.resourceName(clusterName + SUFFIX);
  }

  static boolean isMaxRetriesReached(StackGresDbOps dbOps) {
    return Optional.of(dbOps)
        .map(StackGresDbOps::getStatus)
        .map(StackGresDbOpsStatus::getOpRetries)
        .orElse(0) >= Optional.of(dbOps)
        .map(StackGresDbOps::getSpec)
        .map(StackGresDbOpsSpec::getMaxRetries)
        .orElse(0);
  }

  static boolean isFailed(StackGresDbOps dbOps) {
    return Optional.of(dbOps)
        .map(StackGresDbOps::getStatus)
        .map(StackGresDbOpsStatus::getConditions)
        .stream()
        .flatMap(List::stream)
        .anyMatch(Predicates.and(
            DbOpsStatusCondition.Type.FAILED::isCondition,
            DbOpsStatusCondition.Status.TRUE::isCondition));
  }

  static String jobName(StackGresDbOps dbOps) {
    return jobName(dbOps, getKebabCaseOperation(dbOps));
  }

  static String jobName(StackGresDbOps dbOps, String operation) {
    String name = dbOps.getMetadata().getName();
    UUID uid = UUID.fromString(dbOps.getMetadata().getUid());
    return ResourceUtil.resourceName(name + "-" + operation + "-"
        + Long.toHexString(uid.getMostSignificantBits())
        + "-" + getCurrentRetry(dbOps));
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

  static Integer getCurrentRetry(StackGresDbOps dbOps) {
    return Optional.of(dbOps)
        .map(StackGresDbOps::getStatus)
        .map(StackGresDbOpsStatus::getOpRetries)
        .map(r -> r + (DbOpsUtil.isFailed(dbOps) && !DbOpsUtil.isMaxRetriesReached(dbOps) ? 1 : 0))
        .orElse(0);
  }
}

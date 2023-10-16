/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardeddbops.ShardedDbOpsStatusCondition.Status;
import io.stackgres.common.crd.sgshardeddbops.ShardedDbOpsStatusCondition.Type;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsSpec;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsStatus;
import io.stackgres.operatorframework.resource.ResourceUtil;

public interface ShardedDbOpsUtil {

  Pattern UPPERCASE_LETTER_PATTERN = Pattern.compile("([A-Z])");

  String SUFFIX = "-dbops";

  static String roleName(StackGresShardedCluster cluster) {
    return roleName(cluster.getMetadata().getName());
  }

  static String roleName(String clusterName) {
    return ResourceUtil.resourceName(clusterName + SUFFIX);
  }

  static boolean isAlreadyCompleted(StackGresShardedDbOps dbOps) {
    return Optional.of(dbOps)
        .map(StackGresShardedDbOps::getStatus)
        .map(StackGresShardedDbOpsStatus::getConditions)
        .stream()
        .flatMap(List::stream)
        .filter(condition -> Status.TRUE.getStatus().equals(condition.getStatus()))
        .anyMatch(condition -> Type.COMPLETED.getType().equals(condition.getType())
            || Type.FAILED.getType().equals(condition.getType()));
  }

  static String jobName(StackGresShardedDbOps dbOps) {
    return jobName(dbOps, getKebabCaseOperation(dbOps));
  }

  static String jobName(StackGresShardedDbOps dbOps, String operation) {
    String name = dbOps.getMetadata().getName();
    return ResourceUtil.resourceName(name);
  }

  static String getTimeout(StackGresShardedDbOps dbOps) {
    return Optional.of(dbOps)
        .map(StackGresShardedDbOps::getSpec)
        .map(StackGresShardedDbOpsSpec::getTimeout)
        .map(Duration::parse)
        .map(Duration::getSeconds)
        .map(Object::toString)
        .orElseGet(() -> String.valueOf(Integer.MAX_VALUE));
  }

  static String getKebabCaseOperation(StackGresShardedDbOps dbOps) {
    return UPPERCASE_LETTER_PATTERN
        .matcher(dbOps.getSpec().getOp())
        .replaceAll(m -> "-" + m.group().toLowerCase(Locale.US));
  }

}

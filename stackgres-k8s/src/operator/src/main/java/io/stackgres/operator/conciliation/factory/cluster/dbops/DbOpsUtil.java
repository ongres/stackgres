/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.dbops;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.google.common.base.Predicates;
import io.stackgres.common.crd.sgdbops.DbOpsStatusCondition;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.operatorframework.resource.ResourceUtil;

public class DbOpsUtil {

  public static boolean isMaxRetriesReached(StackGresDbOps dbOps) {
    return Optional.of(dbOps)
        .map(StackGresDbOps::getStatus)
        .map(StackGresDbOpsStatus::getOpRetries)
        .orElse(0) >= Optional.of(dbOps)
        .map(StackGresDbOps::getSpec)
        .map(StackGresDbOpsSpec::getMaxRetries)
        .orElse(1);
  }

  public static boolean isFailed(StackGresDbOps dbOps) {
    return Optional.of(dbOps)
        .map(StackGresDbOps::getStatus)
        .map(StackGresDbOpsStatus::getConditions)
        .stream()
        .flatMap(List::stream)
        .anyMatch(Predicates.and(
            DbOpsStatusCondition.Type.FAILED::isCondition,
            DbOpsStatusCondition.Status.TRUE::isCondition));
  }

  public static String jobName(StackGresDbOps dbOps) {
    String name = dbOps.getMetadata().getName();
    UUID uid = UUID.fromString(dbOps.getMetadata().getUid());
    return ResourceUtil.resourceName(name + "-" + getOperation(dbOps) + "-"
        + Long.toHexString(uid.getMostSignificantBits())
        + "-" + getCurrentRetry(dbOps));
  }

  public static String jobName(StackGresDbOps dbOps, String operation) {
    String name = dbOps.getMetadata().getName();
    UUID uid = UUID.fromString(dbOps.getMetadata().getUid());
    return ResourceUtil.resourceName(name + "-" + operation + "-"
        + Long.toHexString(uid.getMostSignificantBits())
        + "-" + getCurrentRetry(dbOps));
  }

  public static String getTimeout(StackGresDbOps dbOps){
    return Optional.of(dbOps)
        .map(StackGresDbOps::getSpec)
        .map(StackGresDbOpsSpec::getTimeout)
        .map(Duration::parse)
        .map(Duration::getSeconds)
        .map(Object::toString)
        .orElseGet(() -> String.valueOf(Integer.MAX_VALUE));
  }

  private static String getOperation(StackGresDbOps dbOps) {
    return dbOps.getSpec().getOp();
  }

  private static Integer getCurrentRetry(StackGresDbOps dbOps) {
    return Optional.of(dbOps)
        .map(StackGresDbOps::getStatus)
        .map(StackGresDbOpsStatus::getOpRetries)
        .map(r -> r + (DbOpsUtil.isFailed(dbOps) && !DbOpsUtil.isMaxRetriesReached(dbOps) ? 1 : 0))
        .orElse(1);
  }
}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.stackgres.common.ErrorType;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSql;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShards;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.CONSTRAINT_VIOLATION)
public class ShardsScriptsConfigValidator implements ShardedClusterValidator {

  private final String constraintViolationUri = ErrorType
      .getErrorTypeUri(ErrorType.CONSTRAINT_VIOLATION);

  @Override
  public void validate(StackGresShardedClusterReview review) throws ValidationFailed {
    StackGresShardedCluster cluster = review.getRequest().getObject();
    if (review.getRequest().getOperation() == Operation.UPDATE
        || review.getRequest().getOperation() == Operation.CREATE) {
      List<StackGresClusterManagedScriptEntry> scripts = Optional.of(cluster.getSpec())
          .map(StackGresShardedClusterSpec::getShards)
          .map(StackGresShardedClusterShards::getManagedSql)
          .map(StackGresClusterManagedSql::getScripts)
          .orElse(List.of());
      checkIdsUniqueness(scripts);
    }
  }

  private void checkIdsUniqueness(
      List<StackGresClusterManagedScriptEntry> scripts) throws ValidationFailed {
    if (scripts.stream()
        .collect(Collectors.groupingBy(StackGresClusterManagedScriptEntry::getId))
        .values()
        .stream()
        .anyMatch(list -> list.size() > 1)) {
      fail(constraintViolationUri, "Script entries must contain unique ids");
    }
    if (scripts.stream()
        .collect(Collectors.groupingBy(StackGresClusterManagedScriptEntry::getId))
        .keySet()
        .stream()
        .anyMatch(id -> id >= 0 && id <= StackGresShardedClusterUtil.LAST_RESERVER_SCRIPT_ID)) {
      fail(constraintViolationUri, "Script entries must not use reserved ids from 0 to "
          + StackGresShardedClusterUtil.LAST_RESERVER_SCRIPT_ID);
    }
  }

}

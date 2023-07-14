/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import java.util.Optional;

import io.stackgres.common.ErrorType;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class DistributedLogsValidator implements ShardedClusterValidator {

  private final CustomResourceFinder<StackGresDistributedLogs> distributedLogsFinder;

  @Inject
  public DistributedLogsValidator(
      CustomResourceFinder<StackGresDistributedLogs> distributedLogsFinder) {
    this.distributedLogsFinder = distributedLogsFinder;
  }

  @Override
  public void validate(StackGresShardedClusterReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case CREATE: {
        StackGresShardedCluster cluster = review.getRequest().getObject();
        String distributedLogs = Optional.ofNullable(cluster.getSpec().getDistributedLogs())
            .map(StackGresClusterDistributedLogs::getDistributedLogs)
            .orElse(null);
        checkIfDistributedLogsExists(cluster, distributedLogs,
            "Distributed logs " + distributedLogs + " not found");
        break;
      }
      case UPDATE: {
        StackGresShardedCluster cluster = review.getRequest().getObject();
        String distributedLogs = Optional.ofNullable(cluster.getSpec().getDistributedLogs())
            .map(StackGresClusterDistributedLogs::getDistributedLogs)
            .orElse(null);
        checkIfDistributedLogsExists(cluster, distributedLogs,
            "Cannot update to distributed logs " + distributedLogs
            + " because it doesn't exists");
        break;
      }
      default:
    }

  }

  private void checkIfDistributedLogsExists(StackGresShardedCluster cluster,
      String distributedLogs, String onError) throws ValidationFailed {

    String namespace = cluster.getMetadata().getNamespace();

    if (distributedLogs != null) {
      checkIfProvided(distributedLogs, "sgDistributedLogs");
      Optional<StackGresDistributedLogs> distributedLogsOpt = distributedLogsFinder
          .findByNameAndNamespace(
              StackGresUtil.getNameFromRelativeId(distributedLogs),
              StackGresUtil.getNamespaceFromRelativeId(distributedLogs, namespace));

      if (!distributedLogsOpt.isPresent()) {
        fail(onError);
      }
    }
  }

}

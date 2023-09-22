/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.stackgres.common.ErrorType;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class DistributedLogsValidator implements ClusterValidator {

  private final CustomResourceFinder<StackGresDistributedLogs> distributedLogsFinder;

  @Inject
  public DistributedLogsValidator(
      CustomResourceFinder<StackGresDistributedLogs> distributedLogsFinder) {
    this.distributedLogsFinder = distributedLogsFinder;
  }

  @Override
  @SuppressFBWarnings(value = "SF_SWITCH_NO_DEFAULT",
      justification = "False positive")
  public void validate(StackGresClusterReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case CREATE: {
        StackGresCluster cluster = review.getRequest().getObject();
        String distributedLogs = Optional.ofNullable(cluster.getSpec().getDistributedLogs())
            .map(StackGresClusterDistributedLogs::getSgDistributedLogs)
            .orElse(null);
        checkIfDistributedLogsExists(cluster, distributedLogs,
            "Distributed logs " + distributedLogs + " not found");
        break;
      }
      case UPDATE: {
        StackGresCluster cluster = review.getRequest().getObject();
        String distributedLogs = Optional.ofNullable(cluster.getSpec().getDistributedLogs())
            .map(StackGresClusterDistributedLogs::getSgDistributedLogs)
            .orElse(null);
        checkIfDistributedLogsExists(cluster, distributedLogs,
            "Cannot update to distributed logs " + distributedLogs
            + " because it doesn't exists");
        break;
      }
      default:
    }

  }

  private void checkIfDistributedLogsExists(StackGresCluster cluster,
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

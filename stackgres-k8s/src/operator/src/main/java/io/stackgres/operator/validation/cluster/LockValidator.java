/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import static io.stackgres.operatorframework.resource.ResourceUtil.getServiceAccountFromUsername;
import static io.stackgres.operatorframework.resource.ResourceUtil.isServiceAccountUsername;

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.stackgres.common.ErrorType;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CLUSTER_UPDATE)
public class LockValidator implements ClusterValidator {

  final int timeout;

  @Inject
  public LockValidator(OperatorPropertyContext operatorPropertyContext) {
    super();
    this.timeout = operatorPropertyContext.getInt(OperatorProperty.LOCK_TIMEOUT);
  }

  @Override
  public void validate(StackGresClusterReview review) throws ValidationFailed {
    if (Objects.equals(review.getRequest().getSubResource(), "status")) {
      return;
    }
    switch (review.getRequest().getOperation()) {
      case UPDATE: {
        StackGresCluster cluster = review.getRequest().getObject();
        String username = review.getRequest().getUserInfo().getUsername();
        if (StackGresUtil.isLocked(cluster, timeout)
            && (
                username == null
                || !isServiceAccountUsername(username)
                || !Objects.equals(
                    StackGresUtil.getLockServiceAccount(cluster),
                    getServiceAccountFromUsername(username))
                )) {
          fail("Cluster update is forbidden. It is locked by some SGBackup or SGDbOps"
              + " that is currently running. Please, wait for the operation to finish,"
              + " stop the operation by deleting it or wait for the lock timeout to expire.");
        }
        break;
      }
      default:
    }

  }

}

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

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

  final ObjectMapper objectMapper;
  final int duration;

  @Inject
  public LockValidator(OperatorPropertyContext operatorPropertyContext,
      ObjectMapper objectMapper) {
    this.duration = operatorPropertyContext.getInt(OperatorProperty.LOCK_DURATION);
    this.objectMapper = objectMapper;
  }

  @Override
  @SuppressFBWarnings(value = "SF_SWITCH_NO_DEFAULT",
      justification = "False positive")
  public void validate(StackGresClusterReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case UPDATE: {
        StackGresCluster cluster = review.getRequest().getObject();
        StackGresCluster oldCluster = review.getRequest().getOldObject();
        if (Objects.equals(objectMapper.valueToTree(cluster.getSpec()),
            objectMapper.valueToTree(oldCluster.getSpec()))) {
          return;
        }
        String username = review.getRequest().getUserInfo().getUsername();
        if (StackGresUtil.isLocked(cluster)
            && (
                username == null
                || !isServiceAccountUsername(username)
                || !Objects.equals(
                    StackGresUtil.getLockServiceAccount(cluster),
                    getServiceAccountFromUsername(username))
                )
            ) {
          fail("Cluster update is forbidden. It is locked by some SGBackup or SGDbOps"
              + " that is currently running. Please, wait for the operation to finish,"
              + " stop the operation by deleting it or wait for the lock duration of "
              + duration + " seconds to expire.");
        }
        break;
      }
      default:
    }

  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import static io.stackgres.operatorframework.resource.ResourceUtil.getServiceAccountFromUsername;
import static io.stackgres.operatorframework.resource.ResourceUtil.isServiceAccountUsername;

import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.common.ErrorType;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CLUSTER_UPDATE)
public class LockValidator implements ShardedClusterValidator {

  final ObjectMapper objectMapper;
  final int duration;

  @Inject
  public LockValidator(OperatorPropertyContext operatorPropertyContext,
      ObjectMapper objectMapper) {
    this.duration = operatorPropertyContext.getInt(OperatorProperty.LOCK_DURATION);
    this.objectMapper = objectMapper;
  }

  @Override
  public void validate(StackGresShardedClusterReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case UPDATE: {
        StackGresShardedCluster cluster = review.getRequest().getObject();
        StackGresShardedCluster oldCluster = review.getRequest().getOldObject();
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
          fail("SGShardedCluster update is forbidden. It is locked by some SGShardedBackup or SGShardedDbOps"
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

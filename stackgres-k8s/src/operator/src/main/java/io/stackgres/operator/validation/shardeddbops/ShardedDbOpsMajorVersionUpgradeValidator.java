/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardeddbops;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.operator.common.StackGresShardedDbOpsReview;
import io.stackgres.operator.conciliation.cluster.context.ClusterPostgresVersionContextAppender;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CLUSTER_CREATE)
public class ShardedDbOpsMajorVersionUpgradeValidator implements ShardedDbOpsValidator {

  @Override
  public void validate(StackGresShardedDbOpsReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case CREATE:
        StackGresShardedDbOps dbOps = review.getRequest().getObject();
        if (dbOps.getSpec().isOpMajorVersionUpgrade()) {
          String givenPgVersion = dbOps.getSpec().getMajorVersionUpgrade().getPostgresVersion();

          if (ClusterPostgresVersionContextAppender.BUGGY_PG_VERSIONS.keySet().contains(givenPgVersion)) {
            fail("Do not use PostgreSQL " + givenPgVersion + ". "
                + ClusterPostgresVersionContextAppender.BUGGY_PG_VERSIONS.get(givenPgVersion));
          }
        }
        break;
      default:
    }

  }

}

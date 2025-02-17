/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardeddbops;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.operator.common.StackGresShardedDbOpsReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operator.validation.cluster.PostgresConfigValidator;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CLUSTER_CREATE)
public class ShardedDbOpsMajorVersionUpgradeValidator implements ShardedDbOpsValidator {

  @Override
  @SuppressFBWarnings(value = "SF_SWITCH_NO_DEFAULT",
      justification = "False positive")
  public void validate(StackGresShardedDbOpsReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case CREATE:
        StackGresShardedDbOps dbOps = review.getRequest().getObject();
        if (dbOps.getSpec().isOpMajorVersionUpgrade()) {
          String givenPgVersion = dbOps.getSpec().getMajorVersionUpgrade().getPostgresVersion();

          if (PostgresConfigValidator.BUGGY_PG_VERSIONS.keySet().contains(givenPgVersion)) {
            fail("Do not use PostgreSQL " + givenPgVersion + ". "
                + PostgresConfigValidator.BUGGY_PG_VERSIONS.get(givenPgVersion));
          }
        }
        break;
      default:
    }

  }

}

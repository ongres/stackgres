/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.dbops;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.operator.common.StackGresDbOpsReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operator.validation.cluster.PostgresConfigValidator;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CLUSTER_CREATE)
public class DbOpsMajorVersionUpgradeValidator implements DbOpsValidator {

  @Override
  public void validate(StackGresDbOpsReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case CREATE:
        StackGresDbOps dbOps = review.getRequest().getObject();
        if (dbOps.getSpec().isOpMajorVersionUpgrade()) {
          final String givenPgVersion = dbOps.getSpec().getMajorVersionUpgrade().getPostgresVersion();

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

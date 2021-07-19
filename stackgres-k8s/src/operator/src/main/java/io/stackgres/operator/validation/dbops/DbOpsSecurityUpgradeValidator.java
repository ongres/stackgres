/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.dbops;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.stackgres.common.ErrorType;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresDbOpsReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CR_UPDATE)
public class DbOpsSecurityUpgradeValidator implements DbOpsValidator {

  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  @Inject
  public DbOpsSecurityUpgradeValidator(
      CustomResourceFinder<StackGresCluster> clusterFinder) {
    this.clusterFinder = clusterFinder;
  }

  @Override
  public void validate(StackGresDbOpsReview review) throws ValidationFailed {

    switch (review.getRequest().getOperation()) {
      case CREATE:
        StackGresDbOps dbOps = review.getRequest().getObject();
        if (dbOps.getSpec().isOpSecurityUpgrade()) {
          Optional<String> postgresVersion = clusterFinder.findByNameAndNamespace(
              dbOps.getSpec().getSgCluster(), dbOps.getMetadata().getNamespace())
              .map(StackGresCluster::getSpec)
              .map(StackGresClusterSpec::getPostgresVersion);
          if (postgresVersion.map(version -> StackGresComponent.POSTGRESQL.getOrderedVersions()
              .noneMatch(version::equals))
              .orElse(false)) {
            fail("Major version upgrade must be performed on StackGresCluster before performing"
                + " the upgrade since Postgres version " + postgresVersion.get() + " will not be"
                + " supported after the upgrade is completed");
          }
        }
        break;
      default:
    }

  }

}

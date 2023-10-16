/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.dbops;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.DbOpsReview;
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
  @SuppressFBWarnings(value = "SF_SWITCH_NO_DEFAULT",
      justification = "False positive")
  public void validate(DbOpsReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case CREATE:
        StackGresDbOps dbOps = review.getRequest().getObject();
        if (dbOps.getSpec().isOpSecurityUpgrade()) {
          Optional<StackGresCluster> cluster = clusterFinder.findByNameAndNamespace(
              dbOps.getSpec().getSgCluster(), dbOps.getMetadata().getNamespace());
          if (cluster.map(c -> getPostgresFlavorComponent(c).get(c).streamOrderedVersions()
              .noneMatch(c.getSpec().getPostgres().getVersion()::equals))
              .orElse(false)) {
            fail("Major version upgrade must be performed on SGCluster before performing"
                + " the upgrade since Postgres version " + cluster.get().getSpec().getPostgres()
                .getVersion() + " will not be supported after the upgrade is completed");
          }
        }
        break;
      default:
    }
  }

}

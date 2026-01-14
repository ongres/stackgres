/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.dbops;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.Optional;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresDbOpsReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

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
        if (!dbOps.getSpec().isOpSecurityUpgrade()) {
          return;
        }
        Optional<StackGresCluster> foundCluster = clusterFinder.findByNameAndNamespace(
            dbOps.getSpec().getSgCluster(), dbOps.getMetadata().getNamespace());
        if (foundCluster.isEmpty()) {
          return;
        }
        StackGresCluster cluster = foundCluster.get();
        Optional<String> foundVersion = getPostgresFlavorComponent(cluster)
            .get(cluster)
            .findVersion(cluster.getSpec().getPostgres().getVersion());
        if (foundVersion.isEmpty()) {
          return;
        }
        String version = foundVersion.get();
        if (getPostgresFlavorComponent(cluster)
            .get(cluster)
            .streamOrderedVersions()
            .noneMatch(version::equals)) {
          fail("Major version upgrade must be performed on SGCluster before performing"
              + " the upgrade since Postgres version " + version
              + " will not be supported after the upgrade is completed");
        }
        break;
      default:
    }
  }

}

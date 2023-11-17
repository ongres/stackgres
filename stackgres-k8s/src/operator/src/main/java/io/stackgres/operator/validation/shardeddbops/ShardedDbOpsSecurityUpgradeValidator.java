/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardeddbops;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.Optional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.ShardedDbOpsReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CR_UPDATE)
public class ShardedDbOpsSecurityUpgradeValidator implements ShardedDbOpsValidator {

  private final CustomResourceFinder<StackGresShardedCluster> clusterFinder;

  @Inject
  public ShardedDbOpsSecurityUpgradeValidator(
      CustomResourceFinder<StackGresShardedCluster> clusterFinder) {
    this.clusterFinder = clusterFinder;
  }

  @Override
  @SuppressFBWarnings(value = "SF_SWITCH_NO_DEFAULT",
      justification = "False positive")
  public void validate(ShardedDbOpsReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case CREATE:
        StackGresShardedDbOps dbOps = review.getRequest().getObject();
        if (dbOps.getSpec().isOpSecurityUpgrade()) {
          Optional<StackGresShardedCluster> cluster = clusterFinder.findByNameAndNamespace(
              dbOps.getSpec().getSgShardedCluster(), dbOps.getMetadata().getNamespace());
          if (cluster.map(c -> getPostgresFlavorComponent(c).get(c).streamOrderedVersions()
              .noneMatch(c.getSpec().getPostgres().getVersion()::equals))
              .orElse(false)) {
            fail("Major version upgrade must be performed on SGShardedCluster before performing"
                + " the upgrade since Postgres version " + cluster.get().getSpec().getPostgres()
                .getVersion() + " will not be supported after the upgrade is completed");
          }
        }
        break;
      default:
    }
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.dbops;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.stackgres.common.ErrorType;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.component.StackGresContext;
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

  private final StackGresContext context;
  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  @Inject
  public DbOpsSecurityUpgradeValidator(
      StackGresContext context,
      CustomResourceFinder<StackGresCluster> clusterFinder) {
    this.context = context;
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
        List<String> orderedVersions = (StackGresUtil.isRegistryEnabled(cluster)
            ? getPostgresFlavorComponent(cluster).get(cluster)
            : getPostgresFlavorComponent(cluster).getOrThrow(StackGresVersion.LATEST))
            .streamOrderedVersions(context)
            .toList();
        Optional<String> foundVersion = getPostgresFlavorComponent(cluster)
            .get(cluster)
            .findVersion(context, cluster.getSpec().getPostgres().getVersion());
        if (foundVersion.isEmpty()
            || orderedVersions.stream()
            .noneMatch(foundVersion.get()::equals)) {
          final String version = foundVersion.orElse(cluster.getSpec().getPostgres().getVersion());
          fail("Minor or major version upgrade must be performed on SGCluster before performing"
              + " the security upgrade since Postgres version " + version
              + " will not be supported after the upgrade is completed."
              + " Available versions are: " + orderedVersions.stream().collect(Collectors.joining(", ")));
        }
        break;
      default:
    }
  }

}

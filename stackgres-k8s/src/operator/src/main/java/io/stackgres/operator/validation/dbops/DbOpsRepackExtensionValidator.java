/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.dbops;

import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.DbOpsReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.EXTENSION_NOT_FOUND)
public class DbOpsRepackExtensionValidator implements DbOpsValidator {

  private static final String PG_REPACK = "pg_repack";

  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  @Inject
  public DbOpsRepackExtensionValidator(CustomResourceFinder<StackGresCluster> clusterFinder) {
    this.clusterFinder = clusterFinder;
  }

  @Override
  public void validate(DbOpsReview review) throws ValidationFailed {
    if (review.getRequest().getOperation() == Operation.CREATE) {
      StackGresDbOps dbops = review.getRequest().getObject();
      if (dbops.getSpec().isOpRepack()) {
        String sgCluster = dbops.getSpec().getSgCluster();
        String namespace = dbops.getMetadata().getNamespace();

        Optional<StackGresCluster> cluster =
            clusterFinder.findByNameAndNamespace(sgCluster, namespace);

        boolean isSetToInstall = cluster.map(StackGresCluster::getSpec)
            .map(StackGresClusterSpec::getToInstallPostgresExtensions)
            .stream()
            .flatMap(List::stream)
            .anyMatch(ext -> PG_REPACK.equals(ext.getName()));

        if (!isSetToInstall) {
          fail("The \"" + PG_REPACK + "\" extension is not installed in "
              + "the cluster: \"" + sgCluster + "\", please install the extension first.");
        }

        var podStatuses = cluster.map(StackGresCluster::getStatus)
            .map(StackGresClusterStatus::getPodStatuses);
        if (podStatuses.isPresent()) {
          var missingExtensionPods = new TreeSet<String>();
          podStatuses
              .stream()
              .flatMap(List::stream)
              .forEach(podstatus -> {
                missingExtensionPods.add(podstatus.getName());
                podstatus.getInstalledPostgresExtensions()
                    .stream()
                    .filter(repack -> PG_REPACK.equals(repack.getName()))
                    .forEach(ext -> missingExtensionPods.remove(podstatus.getName()));
              });

          if (!missingExtensionPods.isEmpty()) {
            fail("The \"" + PG_REPACK + "\" extension is being installed in "
                + "the cluster: \"" + sgCluster + "\", pending pods: ["
                + String.join(", ", missingExtensionPods) + "], "
                + "please wait for the installation to complete and try again in a few minutes.");
          }
        } else {
          fail("The \"" + PG_REPACK + "\" extension is being installed in "
              + "the cluster: \"" + sgCluster + "\", "
              + "please wait for the installation to complete and try again in a few minutes.");
        }
      }
    }
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedbackup;

import static java.util.Optional.ofNullable;

import java.util.Optional;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupStatus;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardingType;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresShardedBackupReview;
import io.stackgres.operator.resource.NamedResource;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class ShardedClusterValidator implements ShardedBackupValidator {

  private final CustomResourceFinder<StackGresShardedCluster> clusterFinder;

  private final String errorTypeUri = ErrorType
      .getErrorTypeUri(ErrorType.INVALID_CR_REFERENCE);

  @Inject
  public ShardedClusterValidator(
      CustomResourceFinder<StackGresShardedCluster> clusterFinder) {
    this.clusterFinder = clusterFinder;
  }

  @Override
  public void validate(StackGresShardedBackupReview review) throws ValidationFailed {
    StackGresShardedBackup backup = review.getRequest().getObject();

    switch (review.getRequest().getOperation()) {
      case CREATE -> {
        validateShardedCluster(backup, review, backup.getSpec().getSgShardedCluster());
      }
      case UPDATE -> {
        if (!review.getRequest().getOldObject().getSpec().getSgShardedCluster()
            .equals(backup.getSpec().getSgShardedCluster())) {
          final String message = "Backup sgShardedCluster can not be updated.";
          fail(message);
        }
      }
      default -> {
      }
    }
  }

  private void validateShardedCluster(
      StackGresShardedBackup backup,
      StackGresShardedBackupReview review,
      String clusterName) throws ValidationFailed {
    if (hasStatusBackups(backup)) {
      return;
    }

    NamedResource namedResource = getShardedClusterLocation(review);
    Optional<StackGresShardedCluster> clusterOpt = clusterFinder
        .findByNameAndNamespace(namedResource.resource(), namedResource.namespace());

    checkIfShardedClusterExists(clusterOpt, "SGShardedCluster " + clusterName + " not found");
    checkIfShardedClusterSupportBackups(clusterOpt, "SGShardedCluster " + clusterName
        + " do not support sharded backups");
    checkIfShardedClusterHasValidBackupConfig(clusterOpt,
        "SGShardedCluster " + clusterName + " has no backup configuration");
  }

  private boolean hasStatusBackups(StackGresShardedBackup backup) {
    return Optional.of(backup)
        .map(StackGresShardedBackup::getStatus)
        .map(StackGresShardedBackupStatus::getSgBackups)
        .isPresent();
  }

  private void checkIfShardedClusterHasValidBackupConfig(
      Optional<StackGresShardedCluster> clusterOpt,
      String onError) throws ValidationFailed {

    var backupConfig = ofNullable(
        clusterOpt.get().getSpec().getConfigurations().getBackups());

    if (backupConfig.isEmpty() || backupConfig.get().isEmpty()) {
      fail(onError);
    }
  }

  private void checkIfShardedClusterExists(
      Optional<StackGresShardedCluster> clusterOpt, String onError)
      throws ValidationFailed {
    if (clusterOpt.isEmpty()) {
      fail(onError);
    }
  }

  private void checkIfShardedClusterSupportBackups(
      Optional<StackGresShardedCluster> clusterOpt, String onError)
      throws ValidationFailed {
    if (clusterOpt
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getType)
        .map(StackGresShardingType::fromString)
        .map(StackGresShardingType.SHARDING_SPHERE::equals)
        .orElse(false)) {
      fail(onError);
    }
  }

  private NamedResource getShardedClusterLocation(StackGresShardedBackupReview review) {
    StackGresShardedBackup backup = review.getRequest().getObject();
    String cluster = backup.getSpec().getSgShardedCluster();
    if (cluster.contains(".")) {
      String[] clusterLocation = cluster.split("\\.");
      return new NamedResource(clusterLocation[0], clusterLocation[1]);
    } else {
      String namespace = review.getRequest().getObject().getMetadata().getNamespace();
      return new NamedResource(namespace, cluster);
    }
  }

  public void fail(String message) throws ValidationFailed {
    fail(errorTypeUri, message);
  }

}

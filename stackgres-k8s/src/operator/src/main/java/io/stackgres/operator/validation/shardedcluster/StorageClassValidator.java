/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShards;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.INVALID_STORAGE_CLASS)
public class StorageClassValidator implements ShardedClusterValidator {

  private final ResourceFinder<StorageClass> finder;

  @Inject
  public StorageClassValidator(ResourceFinder<StorageClass> finder) {
    this.finder = finder;
  }

  @Override
  public void validate(StackGresShardedClusterReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case CREATE: {
        StackGresShardedCluster cluster = review.getRequest().getObject();
        String coordinatorStorageClass = cluster.getSpec().getCoordinator()
            .getPod().getPersistentVolume().getStorageClass();
        checkIfStorageClassExist(coordinatorStorageClass, "Storage class "
            + coordinatorStorageClass + " not found for coordinator");
        String shardsStorageClass = cluster.getSpec().getShards()
            .getPod().getPersistentVolume().getStorageClass();
        checkIfStorageClassExist(shardsStorageClass, "Storage class "
            + shardsStorageClass + " not found for shards");
        for (var overrideShard : Optional.of(cluster.getSpec().getShards())
            .map(StackGresShardedClusterShards::getOverrides)
            .orElse(List.of())) {
          if (overrideShard.getPodForShards() == null
              || overrideShard.getPodForShards().getPersistentVolume() == null) {
            continue;
          }
          String overrideShardsStorageClass = overrideShard
              .getPodForShards().getPersistentVolume().getStorageClass();
          checkIfStorageClassExist(overrideShardsStorageClass, "Storage class "
              + overrideShardsStorageClass + " not found for shard " + overrideShard.getIndex());
        }
        break;
      }
      case UPDATE: {
        StackGresShardedCluster cluster = review.getRequest().getObject();
        String coordinatorStorageClass = cluster.getSpec().getCoordinator()
            .getPod().getPersistentVolume().getStorageClass();
        checkIfStorageClassExist(coordinatorStorageClass,
            "Cannot update coordinator to storage class "
                + coordinatorStorageClass + " because it doesn't exists");
        String shardsStorageClass = cluster.getSpec().getShards()
            .getPod().getPersistentVolume().getStorageClass();
        checkIfStorageClassExist(shardsStorageClass, "Cannot update shards to storage class "
            + shardsStorageClass + " because it doesn't exists");
        for (var overrideShard : Optional.of(cluster.getSpec().getShards())
            .map(StackGresShardedClusterShards::getOverrides)
            .orElse(List.of())) {
          if (overrideShard.getPodForShards() == null
              || overrideShard.getPodForShards().getPersistentVolume() == null) {
            continue;
          }
          String overrideShardsStorageClass = overrideShard
              .getPodForShards().getPersistentVolume().getStorageClass();
          checkIfStorageClassExist(overrideShardsStorageClass, "Cannot update shard "
              + overrideShard.getIndex() + " to storage class "
              + overrideShardsStorageClass + " because it doesn't exists");
        }
        break;
      }
      default:
    }

  }

  private void checkIfStorageClassExist(String storageClass, String onError)
      throws ValidationFailed {
    if (storageClass != null && !storageClass.isEmpty()
        && !finder.findByName(storageClass).isPresent()) {
      fail(onError);
    }
  }
}

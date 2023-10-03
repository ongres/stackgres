/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShard;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShards;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class ProfileReferenceValidator implements ShardedClusterValidator {

  private final CustomResourceFinder<StackGresProfile> profileFinder;

  @Inject
  public ProfileReferenceValidator(CustomResourceFinder<StackGresProfile> profileFinder) {
    this.profileFinder = profileFinder;
  }

  @Override
  @SuppressFBWarnings(value = "SF_SWITCH_NO_DEFAULT",
      justification = "False positive")
  public void validate(StackGresShardedClusterReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case CREATE: {
        StackGresShardedCluster cluster = review.getRequest().getObject();
        String coordinatorResourceProfile = cluster.getSpec().getCoordinator()
            .getSgInstanceProfile();
        checkIfProfileExists(review, coordinatorResourceProfile,
            "Invalid profile " + coordinatorResourceProfile + " for coordinator");
        String shardsResourceProfile = cluster.getSpec().getShards().getSgInstanceProfile();
        checkIfProfileExists(review, shardsResourceProfile,
            "Invalid profile " + shardsResourceProfile + " for shards");
        for (var overrideShard : Optional.of(cluster.getSpec().getShards())
            .map(StackGresShardedClusterShards::getOverrides)
            .orElse(List.of())) {
          if (overrideShard.getSgInstanceProfile() == null) {
            continue;
          }
          String overrideshardsResourceProfile = overrideShard
              .getSgInstanceProfile();
          checkIfProfileExists(review, overrideshardsResourceProfile,
              "Invalid profile " + overrideshardsResourceProfile + " for shard "
                  + overrideShard.getIndex());
        }
        break;
      }
      case UPDATE: {
        StackGresShardedCluster cluster = review.getRequest().getObject();
        StackGresShardedCluster oldCluster = review.getRequest().getOldObject();
        String coordinatorResourceProfile = cluster.getSpec().getCoordinator()
            .getSgInstanceProfile();
        String oldCoordinatorResourceProfile =
            oldCluster.getSpec().getCoordinator().getSgInstanceProfile();
        if (!coordinatorResourceProfile.equals(oldCoordinatorResourceProfile)) {
          checkIfProfileExists(review, coordinatorResourceProfile,
              "Cannot update coordinator to profile "
                  + coordinatorResourceProfile + " because it doesn't exists");
        }
        String shardsResourceProfile = cluster.getSpec().getShards().getSgInstanceProfile();
        String oldShardsResourceProfile = oldCluster.getSpec().getShards().getSgInstanceProfile();
        if (!shardsResourceProfile.equals(oldShardsResourceProfile)) {
          checkIfProfileExists(review, shardsResourceProfile,
              "Cannot update shards to profile "
                  + shardsResourceProfile + " because it doesn't exists");
        }
        for (var overrideShard : Optional.of(cluster.getSpec().getShards())
            .map(StackGresShardedClusterShards::getOverrides)
            .orElse(List.of())) {
          if (overrideShard.getSgInstanceProfile() == null) {
            continue;
          }
          String overrideshardsResourceProfile = overrideShard
              .getSgInstanceProfile();
          String oldOverrideshardsResourceProfile = Optional.of(oldCluster.getSpec().getShards())
              .map(StackGresShardedClusterShards::getOverrides)
              .stream()
              .flatMap(List::stream)
              .filter(oldOverrideShard -> Objects.equals(
                  oldOverrideShard.getIndex(),
                  overrideShard.getIndex()))
              .findFirst()
              .map(StackGresShardedClusterShard::getSgInstanceProfile)
              .orElse(oldShardsResourceProfile);
          if (!overrideshardsResourceProfile.equals(oldOverrideshardsResourceProfile)) {
            checkIfProfileExists(review, overrideshardsResourceProfile,
                "Cannot update shard " + overrideShard.getIndex() + " to profile "
                    + overrideshardsResourceProfile + " because it doesn't exists");
          }
        }
        break;
      }
      default:
    }

  }

  private void checkIfProfileExists(
      StackGresShardedClusterReview review, String resourceProfile, String onError)
      throws ValidationFailed {
    StackGresShardedCluster cluster = review.getRequest().getObject();
    String namespace = cluster.getMetadata().getNamespace();

    Optional<StackGresProfile> profileOpt = profileFinder
        .findByNameAndNamespace(resourceProfile, namespace);

    if (!profileOpt.isPresent()) {
      fail(onError);
    }
  }

}

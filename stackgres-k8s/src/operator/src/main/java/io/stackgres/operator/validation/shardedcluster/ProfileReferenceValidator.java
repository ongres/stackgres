/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
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
  public void validate(StackGresShardedClusterReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case CREATE: {
        StackGresShardedCluster cluster = review.getRequest().getObject();
        String coordinatorResourceProfile = cluster.getSpec().getCoordinator().getResourceProfile();
        checkIfProfileExists(review, coordinatorResourceProfile,
            "Invalid profile " + coordinatorResourceProfile + " for coordinator");
        String shardsResourceProfile = cluster.getSpec().getShards().getResourceProfile();
        checkIfProfileExists(review, shardsResourceProfile,
            "Invalid profile " + shardsResourceProfile + " for shards");
        break;
      }
      case UPDATE: {
        StackGresShardedCluster cluster = review.getRequest().getObject();
        String coordinatorResourceProfile = cluster.getSpec().getCoordinator().getResourceProfile();
        checkIfProfileExists(review, coordinatorResourceProfile,
            "Cannot update coordinator to profile "
                + coordinatorResourceProfile + " because it doesn't exists");
        String shardsResourceProfile = cluster.getSpec().getShards().getResourceProfile();
        checkIfProfileExists(review, shardsResourceProfile,
            "Cannot update shards to profile "
                + shardsResourceProfile + " because it doesn't exists");
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

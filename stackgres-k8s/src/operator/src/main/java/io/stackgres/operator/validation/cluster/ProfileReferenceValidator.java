/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Optional;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class ProfileReferenceValidator implements ClusterValidator {

  private final CustomResourceFinder<StackGresProfile> profileFinder;

  @Inject
  public ProfileReferenceValidator(CustomResourceFinder<StackGresProfile> profileFinder) {
    this.profileFinder = profileFinder;
  }

  @Override
  public void validate(StackGresClusterReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case CREATE: {
        StackGresCluster cluster = review.getRequest().getObject();
        String resourceProfile = cluster.getSpec().getResourceProfile();
        checkIfProfileExists(review, "Invalid profile " + resourceProfile);
        break;
      }
      case UPDATE: {
        StackGresCluster cluster = review.getRequest().getObject();
        String resourceProfile = cluster.getSpec().getResourceProfile();
        checkIfProfileExists(review, "Cannot update to profile "
            + resourceProfile + " because it doesn't exists");
        break;
      }
      default:
    }

  }

  private void checkIfProfileExists(StackGresClusterReview review, String onError)
      throws ValidationFailed {
    StackGresCluster cluster = review.getRequest().getObject();
    String resourceProfile = cluster.getSpec().getResourceProfile();
    String namespace = cluster.getMetadata().getNamespace();

    Optional<StackGresProfile> profileOpt = profileFinder
        .findByNameAndNamespace(resourceProfile, namespace);

    if (!profileOpt.isPresent()) {
      fail(onError);
    }

  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.operator.common.StackgresClusterReview;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgprofile.StackGresProfile;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@ApplicationScoped
public class ProfileReferenceValidator implements ClusterValidator {

  private CustomResourceFinder<StackGresProfile> profileFinder;

  @Inject
  public ProfileReferenceValidator(CustomResourceFinder<StackGresProfile> profileFinder) {
    this.profileFinder = profileFinder;
  }

  @Override
  public void validate(StackgresClusterReview review) throws ValidationFailed {

    StackGresCluster cluster = review.getRequest().getObject();
    String resourceProfile = cluster.getSpec().getResourceProfile();

    switch (review.getRequest().getOperation()) {
      case CREATE:
        checkIfProfileExists(review, "Invalid profile " + resourceProfile);
        break;
      case UPDATE:
        checkIfProfileExists(review, "Cannot update to profile "
            + resourceProfile + " because it doesn't exists");
        break;
      default:
    }

  }

  private void checkIfProfileExists(StackgresClusterReview review, String onError)
      throws ValidationFailed {
    StackGresCluster cluster = review.getRequest().getObject();
    String resourceProfile = cluster.getSpec().getResourceProfile();
    String namespace = cluster.getMetadata().getNamespace();

    Optional<StackGresProfile> profileOpt = profileFinder
        .findByNameAndNamespace(resourceProfile, namespace);

    if (!profileOpt.isPresent()) {
      throw new ValidationFailed(onError);
    }

  }

}

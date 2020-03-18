/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.stackgres.operator.common.ConfigContext;
import io.stackgres.operator.common.ErrorType;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgprofile.StackGresProfile;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class ProfileReferenceValidator implements ClusterValidator {

  private CustomResourceFinder<StackGresProfile> profileFinder;

  private ConfigContext context;

  @Inject
  public ProfileReferenceValidator(CustomResourceFinder<StackGresProfile> profileFinder,
                                   ConfigContext context) {
    this.profileFinder = profileFinder;
    this.context = context;
  }

  @Override
  public void validate(StackGresClusterReview review) throws ValidationFailed {

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

  private void checkIfProfileExists(StackGresClusterReview review, String onError)
      throws ValidationFailed {
    StackGresCluster cluster = review.getRequest().getObject();
    String resourceProfile = cluster.getSpec().getResourceProfile();
    String namespace = cluster.getMetadata().getNamespace();

    Optional<StackGresProfile> profileOpt = profileFinder
        .findByNameAndNamespace(resourceProfile, namespace);

    if (!profileOpt.isPresent()) {
      fail(context, onError);
    }

  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Optional;
import javax.inject.Inject;

import io.stackgres.common.customresource.sgcluster.StackGresCluster;
import io.stackgres.common.customresource.sgprofile.StackGresProfile;
import io.stackgres.operator.services.StackgresProfileFinder;
import io.stackgres.operator.validation.AdmissionReview;
import io.stackgres.operator.validation.ValidationFailed;

public class ProfileReference implements ClusterValidator {

  private StackgresProfileFinder profileFinder;

  @Inject
  public ProfileReference(StackgresProfileFinder profileFinder) {
    this.profileFinder = profileFinder;
  }

  @Override
  public void validate(AdmissionReview review) throws ValidationFailed {

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

  private void checkIfProfileExists(AdmissionReview review, String onError)
      throws ValidationFailed {
    StackGresCluster cluster = review.getRequest().getObject();
    String resourceProfile = cluster.getSpec().getResourceProfile();

    Optional<StackGresProfile> profileOpt = profileFinder.findProfile(resourceProfile);

    if (!profileOpt.isPresent()) {
      throw new ValidationFailed(onError);
    }

  }

}

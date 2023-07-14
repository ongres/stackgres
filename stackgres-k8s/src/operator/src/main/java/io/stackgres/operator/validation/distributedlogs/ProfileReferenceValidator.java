/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.distributedlogs;

import java.util.Optional;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class ProfileReferenceValidator implements DistributedLogsValidator {

  private final CustomResourceFinder<StackGresProfile> profileFinder;

  @Inject
  public ProfileReferenceValidator(CustomResourceFinder<StackGresProfile> profileFinder) {
    this.profileFinder = profileFinder;
  }

  @Override
  public void validate(StackGresDistributedLogsReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case CREATE: {
        StackGresDistributedLogs distributedLogs = review.getRequest().getObject();
        String resourceProfile = distributedLogs.getSpec().getResourceProfile();
        checkIfProfileExists(review, "Invalid profile " + resourceProfile);
        break;
      }
      case UPDATE: {
        StackGresDistributedLogs distributedLogs = review.getRequest().getObject();
        String resourceProfile = distributedLogs.getSpec().getResourceProfile();
        checkIfProfileExists(review, "Cannot update to profile "
            + resourceProfile + " because it doesn't exists");
        break;
      }
      default:
    }

  }

  private void checkIfProfileExists(StackGresDistributedLogsReview review, String onError)
      throws ValidationFailed {
    StackGresDistributedLogs distributedLogs = review.getRequest().getObject();
    String resourceProfile = distributedLogs.getSpec().getResourceProfile();
    String namespace = distributedLogs.getMetadata().getNamespace();

    Optional<StackGresProfile> profileOpt = profileFinder
        .findByNameAndNamespace(resourceProfile, namespace);

    if (!profileOpt.isPresent()) {
      fail(onError);
    }

  }

}

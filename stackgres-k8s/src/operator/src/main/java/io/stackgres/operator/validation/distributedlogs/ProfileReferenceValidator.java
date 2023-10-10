/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.distributedlogs;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.validation.AbstractReferenceValidator;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class ProfileReferenceValidator
    extends AbstractReferenceValidator<
      StackGresDistributedLogs, StackGresDistributedLogsReview, StackGresProfile>
    implements DistributedLogsValidator {

  @Inject
  public ProfileReferenceValidator(CustomResourceFinder<StackGresProfile> profileFinder) {
    super(profileFinder);
  }

  @Override
  protected Class<StackGresProfile> getReferenceClass() {
    return StackGresProfile.class;
  }

  @Override
  protected String getReference(StackGresDistributedLogs resource) {
    return resource.getSpec().getSgInstanceProfile();
  }

  @Override
  protected boolean checkReferenceFilter(StackGresDistributedLogsReview review) {
    return !Optional.ofNullable(review.getRequest().getDryRun()).orElse(false);
  }

  @Override
  protected void onNotFoundReference(String message) throws ValidationFailed {
    fail(message);
  }

}

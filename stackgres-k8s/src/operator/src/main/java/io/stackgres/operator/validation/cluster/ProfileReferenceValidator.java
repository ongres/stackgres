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
import io.stackgres.operator.validation.AbstractReferenceValidator;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class ProfileReferenceValidator
    extends AbstractReferenceValidator<
      StackGresCluster, StackGresClusterReview, StackGresProfile>
    implements ClusterValidator {

  @Inject
  public ProfileReferenceValidator(CustomResourceFinder<StackGresProfile> profileFinder) {
    super(profileFinder);
  }

  @Override
  protected Class<StackGresProfile> getReferenceClass() {
    return StackGresProfile.class;
  }

  @Override
  protected String getReference(StackGresCluster resource) {
    return resource.getSpec().getSgInstanceProfile();
  }

  @Override
  protected boolean checkReferenceFilter(StackGresClusterReview review) {
    return !Optional.ofNullable(review.getRequest().getDryRun()).orElse(false);
  }

  @Override
  protected void onNotFoundReference(String message) throws ValidationFailed {
    fail(message);
  }

}

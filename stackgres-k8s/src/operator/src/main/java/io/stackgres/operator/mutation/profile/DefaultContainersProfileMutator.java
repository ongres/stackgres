/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.profile;

import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.common.StackGresInstanceProfileReview;
import io.stackgres.operator.initialization.DefaultProfileFactory;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultContainersProfileMutator implements ProfileMutator {

  private final DefaultProfileFactory defaultProfileFactory;

  public DefaultContainersProfileMutator(DefaultProfileFactory defaultProfileFactory) {
    this.defaultProfileFactory = defaultProfileFactory;
  }

  @Override
  public StackGresProfile mutate(StackGresInstanceProfileReview review, StackGresProfile resource) {
    if (review.getRequest().getOperation() == Operation.CREATE
        || review.getRequest().getOperation() == Operation.UPDATE) {
      defaultProfileFactory.setDefaults(resource);
    }

    return resource;
  }

}

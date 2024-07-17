/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.profile;

import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.common.StackGresInstanceProfileReview;
import io.stackgres.operator.mutation.AbstractMutationPipeline;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class ProfilePipeline
    extends AbstractMutationPipeline<StackGresProfile, StackGresInstanceProfileReview> {

  @Inject
  public ProfilePipeline(
      @Any Instance<ProfileMutator> mutators) {
    super(mutators);
  }

}

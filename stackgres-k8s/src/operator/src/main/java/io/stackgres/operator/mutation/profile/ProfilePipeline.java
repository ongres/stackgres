/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.profile;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operator.mutation.AbstractMutationPipeline;

@ApplicationScoped
public class ProfilePipeline
    extends AbstractMutationPipeline<StackGresProfile, SgProfileReview> {

  @Inject
  public ProfilePipeline(
      @Any Instance<ProfileMutator> mutators) {
    super(mutators);
  }

}

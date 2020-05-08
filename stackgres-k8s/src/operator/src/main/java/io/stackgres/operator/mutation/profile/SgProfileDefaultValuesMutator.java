/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.profile;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operator.mutation.DefaultValuesMutator;

@ApplicationScoped
public class SgProfileDefaultValuesMutator
    extends DefaultValuesMutator<StackGresProfile, SgProfileReview>
    implements ProfileMutator {

  @Override
  public List<JsonPatchOperation> mutate(SgProfileReview review) {

    return mutate(SG_PROFILE_CONFIG_POINTER, review.getRequest().getObject());

  }

}

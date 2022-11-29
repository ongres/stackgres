/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.profile;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.DefaultValuesMutator;

@ApplicationScoped
public class SgProfileDefaultValuesMutator
    extends DefaultValuesMutator<StackGresProfile, SgProfileReview>
    implements ProfileMutator {

  @Inject
  public SgProfileDefaultValuesMutator(
      DefaultCustomResourceFactory<StackGresProfile> factory,
      ObjectMapper jsonMapper) {
    super(factory, jsonMapper);
  }

  @Override
  public JsonNode getSourceNode(StackGresProfile resource) {
    return toNode(resource)
        .get("spec");
  }

  @Override
  public JsonNode getTargetNode(StackGresProfile resource) {
    return getSourceNode(resource);
  }

  @Override
  public List<JsonPatchOperation> mutate(SgProfileReview review) {
    return mutate(SPEC_POINTER, review.getRequest().getObject());
  }

}

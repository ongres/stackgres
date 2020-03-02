/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.profile;

import com.fasterxml.jackson.databind.JsonNode;
import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operator.customresource.sgprofile.StackGresProfile;
import io.stackgres.operator.customresource.sgprofile.StackGresProfileSpec;
import io.stackgres.operator.mutation.DefaultValuesMutator;
import io.stackgres.operator.mutation.DefaultValuesMutatorTest;
import io.stackgres.operator.utils.JsonUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SgProfileDefaultValuesMutatorTest extends DefaultValuesMutatorTest<StackGresProfile, SgProfileReview> {

  @Override
  protected DefaultValuesMutator<StackGresProfile, SgProfileReview> getMutatorInstance() {
    return new SgProfileDefaultValuesMutator();
  }

  @Override
  protected SgProfileReview getEmptyReview() {
    SgProfileReview review = JsonUtil
        .readFromJson("sgprofile_allow_request/create.json", SgProfileReview.class);
    review.getRequest().getObject().setSpec(new StackGresProfileSpec());
    return review;
  }

  @Override
  protected SgProfileReview getDefaultReview() {
    return JsonUtil
        .readFromJson("sgprofile_allow_request/create.json", SgProfileReview.class);
  }

  @Override
  protected StackGresProfile getDefaultResource() {
    return JsonUtil.readFromJson("stackgres_profiles/size-xs.json", StackGresProfile.class);
  }

  @Override
  protected JsonNode getConfJson(JsonNode crJson) {
    return crJson.get("spec");
  }
}
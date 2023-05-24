/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.profile;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileSpec;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.AbstractValuesMutator;
import io.stackgres.operator.mutation.DefaultValuesMutatorTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SgProfileDefaultValuesMutatorTest
    extends DefaultValuesMutatorTest<StackGresProfile, SgProfileReview> {

  @Override
  protected AbstractValuesMutator<StackGresProfile, SgProfileReview> getMutatorInstance(
      DefaultCustomResourceFactory<StackGresProfile> factory, JsonMapper jsonMapper) {
    return new SgProfileDefaultValuesMutator(factory, jsonMapper);
  }

  @Override
  protected SgProfileReview getEmptyReview() {
    SgProfileReview review = AdmissionReviewFixtures.instanceProfile().loadCreate().get();
    review.getRequest().getObject().setSpec(new StackGresProfileSpec());
    return review;
  }

  @Override
  protected SgProfileReview getDefaultReview() {
    SgProfileReview review = AdmissionReviewFixtures.instanceProfile().loadCreate().get();
    review.getRequest().getObject().getSpec().setContainers(null);
    review.getRequest().getObject().getSpec().setInitContainers(null);
    return review;
  }

  @Override
  protected StackGresProfile getDefaultResource() {
    var profile = Fixtures.instanceProfile().loadSizeXs().get();
    profile.getSpec().setContainers(null);
    profile.getSpec().setInitContainers(null);
    return profile;
  }

}

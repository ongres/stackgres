/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.profile;

import java.io.IOException;

import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultContainersProfileMutatorTest {

  private StackGresProfile defaultProfile;
  private SgProfileReview review;
  private DefaultContainersProfileMutator mutator;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IOException {
    review = AdmissionReviewFixtures.instanceProfile().loadCreate().get();

    defaultProfile = Fixtures.instanceProfile().loadSizeXs().get();

    mutator = new DefaultContainersProfileMutator(() -> defaultProfile);
    mutator.init();
  }

  @Test
  void alreadyFilledContainersProfiles_shouldSetNothing() throws Exception {
    StackGresProfile expectedProfile = JsonUtil.copy(review.getRequest().getObject());

    StackGresProfile result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        JsonUtil.toJson(expectedProfile),
        JsonUtil.toJson(result));
  }

  @Test
  void missingContainersProfiles_shouldSetThem() throws Exception {
    StackGresProfile expectedProfile = JsonUtil.copy(review.getRequest().getObject());
    review.getRequest().getObject().getSpec().setContainers(null);
    review.getRequest().getObject().getSpec().setInitContainers(null);

    StackGresProfile result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        JsonUtil.toJson(expectedProfile),
        JsonUtil.toJson(result));
  }

  @Test
  void missingContainersCpus_shouldSetThem() throws Exception {
    StackGresProfile expectedProfile = JsonUtil.copy(review.getRequest().getObject());
    review.getRequest().getObject().getSpec().getContainers().values()
        .forEach(container -> container.setCpu(null));
    review.getRequest().getObject().getSpec().getInitContainers().values()
        .forEach(container -> container.setCpu(null));

    StackGresProfile result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        JsonUtil.toJson(expectedProfile),
        JsonUtil.toJson(result));
  }

  @Test
  void missingContainersMemories_shouldSetThem() throws Exception {
    StackGresProfile expectedProfile = JsonUtil.copy(review.getRequest().getObject());
    review.getRequest().getObject().getSpec().getContainers().values()
        .forEach(container -> container.setMemory(null));
    review.getRequest().getObject().getSpec().getInitContainers().values()
        .forEach(container -> container.setMemory(null));

    StackGresProfile result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        JsonUtil.toJson(expectedProfile),
        JsonUtil.toJson(result));
  }

}

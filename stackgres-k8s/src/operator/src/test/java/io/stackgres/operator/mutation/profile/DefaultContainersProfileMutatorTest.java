/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.profile;

import java.io.IOException;
import java.util.Random;

import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileContainer;
import io.stackgres.common.crd.sgprofile.StackGresProfileSpec;
import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultContainersProfileMutatorTest {

  private SgProfileReview review;
  private DefaultContainersProfileMutator mutator;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IOException {
    review = AdmissionReviewFixtures.instanceProfile().loadCreate().get();

    mutator = new DefaultContainersProfileMutator();
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
  void emptyProfiles_shouldSetOnlySections() throws Exception {
    StackGresProfile expectedProfile = JsonUtil.copy(review.getRequest().getObject());
    review.getRequest().getObject().setSpec(new StackGresProfileSpec());
    expectedProfile.getSpec().setCpu(null);
    expectedProfile.getSpec().setMemory(null);
    expectedProfile.getSpec().getContainers().values()
        .forEach(container -> container.setCpu(null));
    expectedProfile.getSpec().getContainers().values()
        .forEach(container -> container.setMemory(null));
    expectedProfile.getSpec().getInitContainers().values()
        .forEach(container -> container.setCpu(null));
    expectedProfile.getSpec().getInitContainers().values()
        .forEach(container -> container.setMemory(null));
    expectedProfile.getSpec().getRequests().setCpu(null);
    expectedProfile.getSpec().getRequests().setMemory(null);
    expectedProfile.getSpec().getRequests().getContainers().values()
        .forEach(container -> container.setCpu(null));
    expectedProfile.getSpec().getRequests().getContainers().values()
        .forEach(container -> container.setMemory(null));
    expectedProfile.getSpec().getRequests().getInitContainers().values()
        .forEach(container -> container.setCpu(null));
    expectedProfile.getSpec().getRequests().getInitContainers().values()
        .forEach(container -> container.setMemory(null));

    StackGresProfile result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        JsonUtil.toJson(expectedProfile),
        JsonUtil.toJson(result));
  }

  @Test
  void missingRequestsProfiles_shouldSetThem() throws Exception {
    StackGresProfile expectedProfile = JsonUtil.copy(review.getRequest().getObject());
    review.getRequest().getObject().getSpec().setRequests(null);

    StackGresProfile result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        JsonUtil.toJson(expectedProfile),
        JsonUtil.toJson(result));
  }

  @Test
  void missingLimitsContainersProfiles_shouldSetThem() throws Exception {
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
  void missingRequestsContainersProfiles_shouldSetThem() throws Exception {
    StackGresProfile expectedProfile = JsonUtil.copy(review.getRequest().getObject());
    review.getRequest().getObject().getSpec().getRequests().setContainers(null);
    review.getRequest().getObject().getSpec().getRequests().setInitContainers(null);

    StackGresProfile result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        JsonUtil.toJson(expectedProfile),
        JsonUtil.toJson(result));
  }

  @Test
  void missingSingleLimitsContainersProfiles_shouldNotSetIt() throws Exception {
    var keys = review.getRequest().getObject().getSpec().getContainers()
        .keySet().stream().toList();
    review.getRequest().getObject().getSpec().getContainers()
        .put(keys.get(new Random().nextInt(keys.size())), new StackGresProfileContainer());
    StackGresProfile expectedProfile = JsonUtil.copy(review.getRequest().getObject());

    StackGresProfile result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        JsonUtil.toJson(expectedProfile),
        JsonUtil.toJson(result));
  }

  @Test
  void missingSingleRequestsContainersProfiles_shouldNotSetIt() throws Exception {
    var keys = review.getRequest().getObject().getSpec().getContainers()
        .keySet().stream().toList();
    review.getRequest().getObject().getSpec().getRequests().getContainers()
        .put(keys.get(new Random().nextInt(keys.size())), new StackGresProfileContainer());
    StackGresProfile expectedProfile = JsonUtil.copy(review.getRequest().getObject());

    StackGresProfile result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        JsonUtil.toJson(expectedProfile),
        JsonUtil.toJson(result));
  }

  @Test
  void missingLimitsContainersCpus_shouldSetOnlyForInitContainers() throws Exception {
    review.getRequest().getObject().getSpec().getContainers().values()
        .forEach(container -> container.setCpu(null));
    StackGresProfile expectedProfile = JsonUtil.copy(review.getRequest().getObject());
    review.getRequest().getObject().getSpec().getInitContainers().values()
        .forEach(container -> container.setCpu(null));

    StackGresProfile result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        JsonUtil.toJson(expectedProfile),
        JsonUtil.toJson(result));
  }

  @Test
  void missingLimitsContainersMemories_shouldSetOnlyForInitContainers() throws Exception {
    review.getRequest().getObject().getSpec().getContainers().values()
        .forEach(container -> container.setMemory(null));
    StackGresProfile expectedProfile = JsonUtil.copy(review.getRequest().getObject());
    review.getRequest().getObject().getSpec().getInitContainers().values()
        .forEach(container -> container.setMemory(null));

    StackGresProfile result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        JsonUtil.toJson(expectedProfile),
        JsonUtil.toJson(result));
  }

  @Test
  void missingRequestsContainersCpus_shouldSetOnlyForInitContainers() throws Exception {
    review.getRequest().getObject().getSpec().getRequests().getContainers().values()
        .forEach(container -> container.setCpu(null));
    StackGresProfile expectedProfile = JsonUtil.copy(review.getRequest().getObject());
    review.getRequest().getObject().getSpec().getRequests().getInitContainers().values()
        .forEach(container -> container.setCpu(null));

    StackGresProfile result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        JsonUtil.toJson(expectedProfile),
        JsonUtil.toJson(result));
  }

  @Test
  void missingRequestsContainersMemories_shouldSetOnlyForInitContainers() throws Exception {
    review.getRequest().getObject().getSpec().getRequests().getContainers().values()
        .forEach(container -> container.setMemory(null));
    StackGresProfile expectedProfile = JsonUtil.copy(review.getRequest().getObject());
    review.getRequest().getObject().getSpec().getRequests().getInitContainers().values()
        .forEach(container -> container.setMemory(null));

    StackGresProfile result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        JsonUtil.toJson(expectedProfile),
        JsonUtil.toJson(result));
  }

  @Test
  void changingLimits_shouldChangeOnlyForInitContainers() throws Exception {
    review = AdmissionReviewFixtures.instanceProfile().loadUpdate().get();
    StackGresProfile expectedProfile = JsonUtil.copy(review.getRequest().getObject());

    StackGresProfile result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        JsonUtil.toJson(expectedProfile),
        JsonUtil.toJson(result));
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Random;

import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresInitContainer;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileContainer;
import io.stackgres.common.crd.sgprofile.StackGresProfileSpec;
import io.stackgres.operator.common.StackGresInstanceProfileReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.initialization.DefaultProfileFactory;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultContainersProfileMutatorTest {

  private StackGresInstanceProfileReview review;
  private DefaultContainersProfileMutator mutator;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IOException {
    review = AdmissionReviewFixtures.instanceProfile().loadCreate().get();

    DefaultProfileFactory defaultProfileFactory = new DefaultProfileFactory();
    mutator = new DefaultContainersProfileMutator(defaultProfileFactory);
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
  void missingLimitsContainersCpus_shouldNotSet() throws Exception {
    review.getRequest().getObject().getSpec().getContainers().values()
        .forEach(container -> container.setCpu(null));
    review.getRequest().getObject().getSpec().getInitContainers().values()
        .forEach(container -> container.setCpu(null));
    StackGresProfile expectedProfile = JsonUtil.copy(review.getRequest().getObject());

    StackGresProfile result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        JsonUtil.toJson(expectedProfile),
        JsonUtil.toJson(result));
  }

  @Test
  void missingLimitsContainersMemories_shouldNotSet() throws Exception {
    review.getRequest().getObject().getSpec().getContainers().values()
        .forEach(container -> container.setMemory(null));
    review.getRequest().getObject().getSpec().getInitContainers().values()
        .forEach(container -> container.setMemory(null));
    StackGresProfile expectedProfile = JsonUtil.copy(review.getRequest().getObject());

    StackGresProfile result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        JsonUtil.toJson(expectedProfile),
        JsonUtil.toJson(result));
  }

  @Test
  void missingRequestsContainersCpus_shouldNotSet() throws Exception {
    review.getRequest().getObject().getSpec().getRequests().getContainers().values()
        .forEach(container -> container.setCpu(null));
    review.getRequest().getObject().getSpec().getRequests().getInitContainers().values()
        .forEach(container -> container.setCpu(null));
    StackGresProfile expectedProfile = JsonUtil.copy(review.getRequest().getObject());

    StackGresProfile result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        JsonUtil.toJson(expectedProfile),
        JsonUtil.toJson(result));
  }

  @Test
  void missingRequestsContainersMemories_shouldNotSet() throws Exception {
    review.getRequest().getObject().getSpec().getRequests().getContainers().values()
        .forEach(container -> container.setMemory(null));
    review.getRequest().getObject().getSpec().getRequests().getInitContainers().values()
        .forEach(container -> container.setMemory(null));
    StackGresProfile expectedProfile = JsonUtil.copy(review.getRequest().getObject());

    StackGresProfile result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        JsonUtil.toJson(expectedProfile),
        JsonUtil.toJson(result));
  }

  @Test
  void changedLimits_shouldRecomputeStaleDerivedValuesButKeepCustomizations() throws Exception {
    review = AdmissionReviewFixtures.instanceProfile().loadUpdate().get();
    StackGresProfile oldProfile = review.getRequest().getOldObject();
    oldProfile.getSpec().getContainers().clear();
    oldProfile.getSpec().getInitContainers().clear();
    oldProfile.getSpec().getRequests().getContainers().clear();
    oldProfile.getSpec().getRequests().getInitContainers().clear();
    new DefaultProfileFactory().setDefaults(oldProfile);
    StackGresProfile staleProfile = JsonUtil.copy(oldProfile);
    staleProfile.getSpec().setCpu("500m");
    staleProfile.getSpec().setMemory("512Mi");
    staleProfile.getSpec().getRequests().setCpu("500m");
    staleProfile.getSpec().getRequests().setMemory("512Mi");
    staleProfile.getSpec().getContainers()
        .get(StackGresContainer.ENVOY.getNameWithPrefix()).setCpu("750m");
    review.getRequest().setObject(staleProfile);

    StackGresProfile result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    assertEquals("500m", result.getSpec().getInitContainers()
        .get(StackGresInitContainer.SETUP_FILESYSTEM.getNameWithPrefix()).getCpu());
    assertEquals("512Mi", result.getSpec().getInitContainers()
        .get(StackGresInitContainer.SETUP_FILESYSTEM.getNameWithPrefix()).getMemory());
    assertEquals("500m", result.getSpec().getRequests().getInitContainers()
        .get(StackGresInitContainer.SETUP_FILESYSTEM.getNameWithPrefix()).getCpu());
    assertEquals("512Mi", result.getSpec().getRequests().getInitContainers()
        .get(StackGresInitContainer.SETUP_FILESYSTEM.getNameWithPrefix()).getMemory());
    assertEquals("750m", result.getSpec().getContainers()
        .get(StackGresContainer.ENVOY.getNameWithPrefix()).getCpu());
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

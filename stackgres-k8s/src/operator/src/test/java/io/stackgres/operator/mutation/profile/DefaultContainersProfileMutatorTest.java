/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.profile;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonpatch.AddOperation;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.github.fge.jsonpatch.ReplaceOperation;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.common.SgProfileReview;
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
    review = JsonUtil.readFromJson("sgprofile_allow_request/create.json",
        SgProfileReview.class);

    defaultProfile = JsonUtil.readFromJson("stackgres_profiles/size-xs.json",
        StackGresProfile.class);

    mutator = new DefaultContainersProfileMutator(() -> defaultProfile);
    mutator.init();
  }

  @Test
  void alreadyFilledContainersProfiles_shouldSetNothing() throws Exception {
    StackGresProfile expectedProfile = JsonUtil.copy(review.getRequest().getObject());

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertTrue(operations.isEmpty());

    JsonNode crJson = JsonUtil.toJson(review.getRequest().getObject());
    JsonNode newConfig = new JsonPatch(operations).apply(crJson);
    StackGresProfile actualProfile = JsonUtil.fromJson(newConfig, StackGresProfile.class);

    JsonUtil.assertJsonEquals(
        JsonUtil.toJson(expectedProfile),
        JsonUtil.toJson(actualProfile));
  }

  @Test
  void missingContainersProfiles_shouldSetThem() throws Exception {
    StackGresProfile expectedProfile = JsonUtil.copy(review.getRequest().getObject());
    review.getRequest().getObject().getSpec().setContainers(null);
    review.getRequest().getObject().getSpec().setInitContainers(null);

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(2, operations.size());
    assertEquals(2, operations.stream().filter(AddOperation.class::isInstance).count());

    JsonNode crJson = JsonUtil.toJson(review.getRequest().getObject());
    JsonNode newConfig = new JsonPatch(operations).apply(crJson);
    StackGresProfile actualProfile = JsonUtil.fromJson(newConfig, StackGresProfile.class);

    JsonUtil.assertJsonEquals(
        JsonUtil.toJson(expectedProfile),
        JsonUtil.toJson(actualProfile));
  }

  @Test
  void missingContainersCpus_shouldSetThem() throws Exception {
    StackGresProfile expectedProfile = JsonUtil.copy(review.getRequest().getObject());
    review.getRequest().getObject().getSpec().getContainers().values()
        .forEach(container -> container.setCpu(null));
    review.getRequest().getObject().getSpec().getInitContainers().values()
        .forEach(container -> container.setCpu(null));

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(2, operations.size());
    assertEquals(2, operations.stream().filter(ReplaceOperation.class::isInstance).count());

    JsonNode crJson = JsonUtil.toJson(review.getRequest().getObject());
    JsonNode newConfig = new JsonPatch(operations).apply(crJson);
    StackGresProfile actualProfile = JsonUtil.fromJson(newConfig, StackGresProfile.class);

    JsonUtil.assertJsonEquals(
        JsonUtil.toJson(expectedProfile),
        JsonUtil.toJson(actualProfile));
  }

  @Test
  void missingContainersMemories_shouldSetThem() throws Exception {
    StackGresProfile expectedProfile = JsonUtil.copy(review.getRequest().getObject());
    review.getRequest().getObject().getSpec().getContainers().values()
        .forEach(container -> container.setMemory(null));
    review.getRequest().getObject().getSpec().getInitContainers().values()
        .forEach(container -> container.setMemory(null));

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(2, operations.size());
    assertEquals(2, operations.stream().filter(ReplaceOperation.class::isInstance).count());

    JsonNode crJson = JsonUtil.toJson(review.getRequest().getObject());
    JsonNode newConfig = new JsonPatch(operations).apply(crJson);
    StackGresProfile actualProfile = JsonUtil.fromJson(newConfig, StackGresProfile.class);

    JsonUtil.assertJsonEquals(
        JsonUtil.toJson(expectedProfile),
        JsonUtil.toJson(actualProfile));
  }

}

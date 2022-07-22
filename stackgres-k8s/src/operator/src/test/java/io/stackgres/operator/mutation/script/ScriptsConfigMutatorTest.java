/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.github.fge.jsonpatch.ReplaceOperation;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import io.stackgres.common.crd.sgscript.StackGresScriptEntryStatus;
import io.stackgres.common.crd.sgscript.StackGresScriptStatus;
import io.stackgres.operator.common.StackGresScriptReview;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScriptsConfigMutatorTest {

  protected static final ObjectMapper JSON_MAPPER = new ObjectMapper();

  private ScriptsConfigMutator mutator = new ScriptsConfigMutator();

  @Test
  void createScriptAlreadyValid_shouldDoNothing() throws JsonPatchException {
    StackGresScriptReview review = JsonUtil
        .readFromJson("script_allow_requests/valid_creation.json", StackGresScriptReview.class);

    JsonNode expectedScript = JsonUtil.toJson(review.getRequest().getObject());

    JsonNode crJson = JsonUtil.toJson(review.getRequest().getObject());

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertTrue(operations.isEmpty());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualScript = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedScript, actualScript);
  }

  @Test
  void createScriptWithNullManagedVersions_shouldSetItToTrue() throws JsonPatchException {
    StackGresScriptReview review = JsonUtil
        .readFromJson("script_allow_requests/valid_creation.json", StackGresScriptReview.class);

    JsonNode expectedScript = JsonUtil.toJson(review.getRequest().getObject());

    review.getRequest().getObject().getSpec().setManagedVersions(null);
    JsonNode crJson = JsonUtil.toJson(review.getRequest().getObject());

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualScript = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedScript, actualScript);
  }

  @Test
  void createScriptWithFalseManagedVersions_shouldDoNothing() throws JsonPatchException {
    StackGresScriptReview review = JsonUtil
        .readFromJson("script_allow_requests/valid_creation.json", StackGresScriptReview.class);
    review.getRequest().getObject().getSpec().setManagedVersions(false);

    JsonNode expectedScript = JsonUtil.toJson(review.getRequest().getObject());

    JsonNode crJson = JsonUtil.toJson(review.getRequest().getObject());

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertTrue(operations.isEmpty());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualScript = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedScript, actualScript);
  }

  @Test
  void createScriptWithoutScriptsAndScriptsStatuses_shouldDoNothing() throws JsonPatchException {
    StackGresScriptReview review = JsonUtil
        .readFromJson("script_allow_requests/valid_creation.json", StackGresScriptReview.class);
    review.getRequest().getObject().getSpec().setScripts(null);
    review.getRequest().getObject().getStatus().setScripts(null);

    JsonNode expectedScript = JsonUtil.toJson(review.getRequest().getObject());

    JsonNode crJson = JsonUtil.toJson(review.getRequest().getObject());

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertTrue(operations.isEmpty());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualScript = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedScript, actualScript);
  }

  @Test
  void createScriptWithoutScriptsAndStatus_shouldDoNothing() throws JsonPatchException {
    StackGresScriptReview review = JsonUtil
        .readFromJson("script_allow_requests/valid_creation.json", StackGresScriptReview.class);
    review.getRequest().getObject().getSpec().setScripts(null);
    review.getRequest().getObject().setStatus(null);

    JsonNode expectedScript = JsonUtil.toJson(review.getRequest().getObject());

    JsonNode crJson = JsonUtil.toJson(review.getRequest().getObject());

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertTrue(operations.isEmpty());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualScript = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedScript, actualScript);
  }

  @Test
  void createScriptWithouIds_shouldAddThem() throws JsonPatchException {
    StackGresScriptReview review = JsonUtil
        .readFromJson("script_allow_requests/valid_creation.json", StackGresScriptReview.class);

    final JsonNode expectedScript = JsonUtil.toJson(review.getRequest().getObject());

    review.getRequest().getObject().getSpec().getScripts().stream()
        .forEach(scriptEntry -> scriptEntry.setId(null));
    review.getRequest().getObject().setStatus(new StackGresScriptStatus());

    JsonNode crJson = JsonUtil.toJson(review.getRequest().getObject());

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(2, operations.size());
    assertEquals(2, operations.stream().filter(o -> o instanceof ReplaceOperation).count());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualScript = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedScript, actualScript);
  }

  @Test
  void updateScriptWithoutModification_shouldDoNothing() throws JsonPatchException {
    StackGresScriptReview review = JsonUtil
        .readFromJson("script_allow_requests/valid_update.json", StackGresScriptReview.class);

    JsonNode expectedScript = JsonUtil.toJson(review.getRequest().getObject());

    JsonNode crJson = JsonUtil.toJson(review.getRequest().getObject());

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertTrue(operations.isEmpty());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualScript = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedScript, actualScript);
  }

  @Test
  void updateScriptRemovingScripts_shouldRemoveTheScriptsStatuses() throws JsonPatchException {
    StackGresScriptReview review = JsonUtil
        .readFromJson("script_allow_requests/valid_update.json", StackGresScriptReview.class);
    review.getRequest().getObject().getSpec().setScripts(null);
    review.getRequest().getOldObject().getSpec().setScripts(null);

    StackGresScript expected = JsonUtil.copy(review.getRequest().getObject());
    expected.getStatus().setScripts(new ArrayList<>());
    JsonNode expectedScript = JsonUtil.toJson(expected);

    JsonNode crJson = JsonUtil.toJson(review.getRequest().getObject());

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualScript = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedScript, actualScript);
  }

  @Test
  void updateScriptWithoutStatusAndScriptsStatuses_shouldDoNothing() throws JsonPatchException {
    StackGresScriptReview review = JsonUtil
        .readFromJson("script_allow_requests/valid_update.json", StackGresScriptReview.class);
    review.getRequest().getObject().getSpec().setScripts(null);
    review.getRequest().getObject().getStatus().setScripts(null);
    review.getRequest().getOldObject().getSpec().setScripts(null);

    JsonNode expectedScript = JsonUtil.toJson(review.getRequest().getObject());

    JsonNode crJson = JsonUtil.toJson(review.getRequest().getObject());

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertTrue(operations.isEmpty());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualScript = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedScript, actualScript);
  }

  @Test
  void updateScriptAddingAnEntry_shouldSetIdAndVersion() throws JsonPatchException {
    StackGresScriptReview review = JsonUtil
        .readFromJson("script_allow_requests/valid_update.json", StackGresScriptReview.class);
    review.getRequest().getObject().getSpec().getScripts().add(1, new StackGresScriptEntry());

    StackGresScript expected = JsonUtil.copy(review.getRequest().getObject());
    expected.getSpec().getScripts().get(1).setId(3);
    expected.getSpec().getScripts().get(1).setVersion(0);
    expected.getStatus().getScripts().add(1, new StackGresScriptEntryStatus());
    expected.getStatus().getScripts().get(1).setId(3);
    JsonNode expectedScript = JsonUtil.toJson(expected);

    JsonNode crJson = JsonUtil.toJson(review.getRequest().getObject());

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(2, operations.size());
    assertEquals(2, operations.stream().filter(o -> o instanceof ReplaceOperation).count());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualScript = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedScript, actualScript);
  }

  @Test
  void updateScriptRemovingAnEntryAndItsStatus_shouldDoNothing() throws JsonPatchException {
    StackGresScriptReview review = JsonUtil
        .readFromJson("script_allow_requests/valid_update.json", StackGresScriptReview.class);
    review.getRequest().getObject().getSpec().getScripts().remove(1);
    review.getRequest().getObject().getStatus().getScripts().remove(1);

    JsonNode expectedScript = JsonUtil.toJson(review.getRequest().getObject());

    JsonNode crJson = JsonUtil.toJson(review.getRequest().getObject());

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertTrue(operations.isEmpty());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualScript = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedScript, actualScript);
  }

}

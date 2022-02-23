/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.AddOperation;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.github.fge.jsonpatch.ReplaceOperation;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
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

    JsonNode expectedScript = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertTrue(operations.isEmpty());

    JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualScript = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedScript, actualScript);
  }

  @Test
  void createScriptWithNullManagedVersions_shouldSetItToTrue() throws JsonPatchException {
    StackGresScriptReview review = JsonUtil
        .readFromJson("script_allow_requests/valid_creation.json", StackGresScriptReview.class);

    JsonNode expectedScript = JSON_MAPPER.valueToTree(review.getRequest().getObject());
    review.getRequest().getObject().getSpec().setManagedVersions(null);

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof AddOperation).count());

    JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualScript = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedScript, actualScript);
  }

  @Test
  void createScriptWithFalseManagedVersions_shouldDoNothing() throws JsonPatchException {
    StackGresScriptReview review = JsonUtil
        .readFromJson("script_allow_requests/valid_creation.json", StackGresScriptReview.class);

    review.getRequest().getObject().getSpec().setManagedVersions(false);
    JsonNode expectedScript = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertTrue(operations.isEmpty());

    JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualScript = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedScript, actualScript);
  }

  @Test
  void createScriptWithNoScripts_shouldDoNothing() throws JsonPatchException {
    StackGresScriptReview review = JsonUtil
        .readFromJson("script_allow_requests/valid_creation.json", StackGresScriptReview.class);

    review.getRequest().getObject().getSpec().setScripts(null);
    review.getRequest().getObject().getStatus().setScripts(null);
    review.getRequest().getObject().getStatus().setLastId(-1);
    JsonNode expectedScript = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertTrue(operations.isEmpty());

    JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualScript = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedScript, actualScript);
  }

  @Test
  void createScriptWithoutLastId_shouldResetStatus() throws JsonPatchException {
    StackGresScriptReview review = JsonUtil
        .readFromJson("script_allow_requests/valid_creation.json", StackGresScriptReview.class);

    review.getRequest().getObject().getSpec().setScripts(null);
    review.getRequest().getObject().getStatus().setScripts(List.of());
    review.getRequest().getObject().getStatus().setLastId(-1);
    JsonNode expectedScript = JSON_MAPPER.valueToTree(review.getRequest().getObject());
    review.getRequest().getObject().getStatus().setLastId(null);

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());

    JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualScript = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedScript, actualScript);
  }

  @Test
  void createScriptWithoutStatus_shouldAddStatus() throws JsonPatchException {
    StackGresScriptReview review = JsonUtil
        .readFromJson("script_allow_requests/valid_creation.json", StackGresScriptReview.class);

    review.getRequest().getObject().getSpec().setScripts(null);
    review.getRequest().getObject().getStatus().setScripts(null);
    review.getRequest().getObject().getStatus().setLastId(-1);

    JsonNode expectedScript = JSON_MAPPER.valueToTree(review.getRequest().getObject());
    ((ObjectNode) expectedScript.get("status")).remove("scripts");

    review.getRequest().getObject().setStatus(null);

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof AddOperation).count());

    JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualScript = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedScript, actualScript);
  }

  @Test
  void createScriptWithouIds_shouldAddThem() throws JsonPatchException {
    StackGresScriptReview review = JsonUtil
        .readFromJson("script_allow_requests/valid_creation.json", StackGresScriptReview.class);
    final JsonNode expectedScript = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    review.getRequest().getObject().getSpec().getScripts().stream()
        .forEach(scriptEntry -> scriptEntry.setId(null));
    review.getRequest().getObject().setStatus(new StackGresScriptStatus());

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(4, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());
    assertEquals(3, operations.stream().filter(o -> o instanceof AddOperation).count());

    JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualScript = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedScript, actualScript);
  }

  @Test
  void updateScriptWithWithoutModification_shouldDoNothing() throws JsonPatchException {
    StackGresScriptReview review = JsonUtil
        .readFromJson("script_allow_requests/valid_update.json", StackGresScriptReview.class);

    JsonNode expectedScript = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertTrue(operations.isEmpty());

    JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualScript = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedScript, actualScript);
  }

  @Test
  void updateScriptWithNoScripts_shouldDoNothing() throws JsonPatchException {
    StackGresScriptReview review = JsonUtil
        .readFromJson("script_allow_requests/valid_update.json", StackGresScriptReview.class);

    review.getRequest().getObject().getSpec().setScripts(null);
    review.getRequest().getObject().getStatus().setLastId(-1);
    review.getRequest().getOldObject().getSpec().setScripts(null);
    review.getRequest().getOldObject().getStatus().setLastId(-1);
    JsonNode expectedScript = JSON_MAPPER.valueToTree(review.getRequest().getObject());
    ((ObjectNode) expectedScript.get("status")).remove("scripts");

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());

    JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualScript = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedScript, actualScript);
  }

  @Test
  void updateScriptWithoutStatus_shouldDoNothing() throws JsonPatchException {
    StackGresScriptReview review = JsonUtil
        .readFromJson("script_allow_requests/valid_update.json", StackGresScriptReview.class);

    review.getRequest().getObject().getSpec().setScripts(null);
    review.getRequest().getObject().getStatus().setScripts(null);
    review.getRequest().getObject().getStatus().setLastId(-1);
    review.getRequest().getOldObject().getSpec().setScripts(null);
    review.getRequest().getOldObject().getStatus().setLastId(-1);
    JsonNode expectedScript = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertTrue(operations.isEmpty());

    JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualScript = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedScript, actualScript);
  }

  @Test
  void updateScriptAddingAnEntry_shouldSetIdAndVersion() throws JsonPatchException {
    StackGresScriptReview review = JsonUtil
        .readFromJson("script_allow_requests/valid_update.json", StackGresScriptReview.class);

    review.getRequest().getObject().getSpec().getScripts().add(1, new StackGresScriptEntry());

    JsonNode expectedScript = JSON_MAPPER.valueToTree(review.getRequest().getObject());
    ((ObjectNode) expectedScript.get("spec").get("scripts").get(1)).put("id", 3);
    ((ObjectNode) expectedScript.get("spec").get("scripts").get(1)).put("version", 0);
    ((ObjectNode) expectedScript.get("status")).put("lastId", 3);
    var statusScripts = ((ArrayNode) ((ObjectNode) expectedScript.get("status")).get("scripts"));
    var statusScript = statusScripts.insertObject(1);
    statusScript.put("id", 3);

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(3, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());
    assertEquals(2, operations.stream().filter(o -> o instanceof AddOperation).count());

    JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualScript = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedScript, actualScript);
  }

  @Test
  void updateScriptRemovingAnEntry_shouldDoNothing() throws JsonPatchException {
    StackGresScriptReview review = JsonUtil
        .readFromJson("script_allow_requests/valid_update.json", StackGresScriptReview.class);

    review.getRequest().getObject().getSpec().getScripts().remove(1);
    review.getRequest().getObject().getStatus().getScripts().remove(1);

    JsonNode expectedScript = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertTrue(operations.isEmpty());

    JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualScript = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedScript, actualScript);
  }

}

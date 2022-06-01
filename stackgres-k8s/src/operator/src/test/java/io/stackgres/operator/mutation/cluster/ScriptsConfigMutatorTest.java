/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

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
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSqlStatus;
import io.stackgres.operator.common.StackGresClusterReview;
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
    StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation_with_managed_sql.json",
            StackGresClusterReview.class);

    JsonNode expectedCluster = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());

    JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualCluster = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedCluster, actualCluster);
  }

  @Test
  void createClusterWithNoScripts_shouldDoNothing() throws JsonPatchException {
    StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation_with_managed_sql.json",
            StackGresClusterReview.class);

    review.getRequest().getObject().getSpec().getManagedSql().setScripts(null);
    review.getRequest().getObject().getStatus().getManagedSql().setScripts(null);
    review.getRequest().getObject().getStatus().getManagedSql().setLastId(-1);
    JsonNode expectedCluster = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertTrue(operations.isEmpty());

    JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualCluster = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedCluster, actualCluster);
  }

  @Test
  void createClusterWithoutLastId_shouldRemoveManagedSql() throws JsonPatchException {
    StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation_with_managed_sql.json",
            StackGresClusterReview.class);

    review.getRequest().getObject().getSpec().getManagedSql().setScripts(null);
    review.getRequest().getObject().getStatus().getManagedSql().setScripts(null);
    review.getRequest().getObject().getStatus().getManagedSql().setLastId(-1);
    JsonNode expectedCluster = JSON_MAPPER.valueToTree(review.getRequest().getObject());
    review.getRequest().getObject().getStatus().setManagedSql(null);

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof AddOperation).count());

    JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualCluster = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedCluster, actualCluster);
  }

  @Test
  void createClusterWithoutStatus_shouldAddStatus() throws JsonPatchException {
    StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation_with_managed_sql.json",
            StackGresClusterReview.class);

    review.getRequest().getObject().getSpec().getManagedSql().setScripts(null);
    review.getRequest().getObject().getStatus().getManagedSql().setScripts(null);
    review.getRequest().getObject().getStatus().getManagedSql().setLastId(-1);

    JsonNode expectedCluster = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    review.getRequest().getObject().setStatus(null);

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof AddOperation).count());

    JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualCluster = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedCluster, actualCluster);
  }

  @Test
  void createClusterWithouIds_shouldAddThem() throws JsonPatchException {
    StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation_with_managed_sql.json",
            StackGresClusterReview.class);
    review.getRequest().getObject().getStatus().getManagedSql().getScripts().get(0)
        .setScripts(null);
    review.getRequest().getObject().getStatus().getManagedSql().getScripts().get(1)
        .setScripts(null);
    review.getRequest().getObject().getStatus().getManagedSql().getScripts().get(2)
        .setScripts(null);
    final JsonNode expectedCluster = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    review.getRequest().getObject().getSpec().getManagedSql().getScripts().stream()
        .forEach(scriptEntry -> scriptEntry.setId(null));
    review.getRequest().getObject().getStatus().setManagedSql(
        new StackGresClusterManagedSqlStatus());

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(4, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());
    assertEquals(3, operations.stream().filter(o -> o instanceof AddOperation).count());

    JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualCluster = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedCluster, actualCluster);
  }

  @Test
  void updateClusterWithWithoutModification_shouldDoNothing() throws JsonPatchException {
    StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_update_with_managed_sql.json",
            StackGresClusterReview.class);

    JsonNode expectedCluster = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());

    JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualCluster = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedCluster, actualCluster);
  }

  @Test
  void updateClusterWithNoScripts_shouldRemoveManagedSql() throws JsonPatchException {
    StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_update_with_managed_sql.json",
            StackGresClusterReview.class);

    review.getRequest().getObject().getSpec().getManagedSql().setScripts(null);
    review.getRequest().getObject().getStatus().getManagedSql().setLastId(-1);
    review.getRequest().getOldObject().getSpec().getManagedSql().setScripts(null);
    review.getRequest().getOldObject().getStatus().getManagedSql().setLastId(-1);
    JsonNode expectedCluster = JSON_MAPPER.valueToTree(review.getRequest().getObject());
    ((ObjectNode) expectedCluster.get("status").get("managedSql")).remove("scripts");

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());

    JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualCluster = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedCluster, actualCluster);
  }

  @Test
  void updateClusterWithoutStatus_shouldDoNothing() throws JsonPatchException {
    StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_update_with_managed_sql.json",
            StackGresClusterReview.class);

    review.getRequest().getObject().getSpec().getManagedSql().setScripts(null);
    review.getRequest().getObject().getStatus().getManagedSql().setScripts(null);
    review.getRequest().getObject().getStatus().getManagedSql().setLastId(-1);
    review.getRequest().getOldObject().getSpec().getManagedSql().setScripts(null);
    review.getRequest().getOldObject().getStatus().getManagedSql().setLastId(-1);
    JsonNode expectedCluster = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertTrue(operations.isEmpty());

    JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualCluster = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedCluster, actualCluster);
  }

  @Test
  void updateClusterAddingAnEntry_shouldSetIdAndVersion() throws JsonPatchException {
    StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_update_with_managed_sql.json",
            StackGresClusterReview.class);

    review.getRequest().getObject().getSpec().getManagedSql().getScripts()
        .add(1, new StackGresClusterManagedScriptEntry());

    JsonNode expectedCluster = JSON_MAPPER.valueToTree(review.getRequest().getObject());
    ((ObjectNode) expectedCluster.get("spec").get("managedSql").get("scripts").get(1))
        .put("id", 3);
    ((ObjectNode) expectedCluster.get("status").get("managedSql")).put("lastId", 3);
    var statusScripts = ((ArrayNode) ((ObjectNode) expectedCluster.get("status").get("managedSql"))
        .get("scripts"));
    var statusScript = statusScripts.insertObject(1);
    statusScript.put("id", 3);

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(2, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());
    assertEquals(1, operations.stream().filter(o -> o instanceof AddOperation).count());

    JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualCluster = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedCluster, actualCluster);
  }

  @Test
  void updateClusterRemovingAnEntry_shouldDoNothing() throws JsonPatchException {
    StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_update_with_managed_sql.json",
            StackGresClusterReview.class);

    review.getRequest().getObject().getSpec().getManagedSql().getScripts().remove(1);
    review.getRequest().getObject().getStatus().getManagedSql().getScripts().remove(1);

    JsonNode expectedCluster = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());

    JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode actualCluster = jp.apply(crJson);

    JsonUtil.assertJsonEquals(expectedCluster, actualCluster);
  }

}

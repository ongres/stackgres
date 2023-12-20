/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.common.ManagedSqlUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSqlStatus;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScriptsConfigMutatorTest {

  protected static final ObjectMapper JSON_MAPPER = new ObjectMapper();

  private ScriptsConfigMutator mutator;

  @BeforeEach
  void setUp() throws Exception {
    mutator = new ScriptsConfigMutator();
  }

  @Test
  void createScriptAlreadyValid_shouldDoNothing() {
    StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadCreateWithManagedSql().get();

    JsonNode expectedCluster = JsonUtil.toJson(review.getRequest().getObject());

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expectedCluster,
        JsonUtil.toJson(result));
  }

  @Test
  void createClusterWithDefaultScript_shouldDoNothing() {
    StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadCreateWithManagedSql().get();

    review.getRequest().getObject().getSpec().getManagedSql().setScripts(
        review.getRequest().getObject().getSpec().getManagedSql().getScripts().subList(0, 1));
    review.getRequest().getObject().getStatus().getManagedSql().setScripts(
        review.getRequest().getObject().getStatus().getManagedSql().getScripts().subList(0, 1));
    JsonNode expectedCluster = JsonUtil.toJson(review.getRequest().getObject());

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expectedCluster,
        JsonUtil.toJson(result));
  }

  @Test
  void createClusterWithoutStatus_shouldAddStatusWithDefaultScript() {
    StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadCreateWithManagedSql().get();

    review.getRequest().getObject().getSpec().getManagedSql().setScripts(null);
    review.getRequest().getObject().getStatus().getManagedSql().setScripts(null);

    StackGresCluster expected = JsonUtil.copy(review.getRequest().getObject());
    expected.getSpec().getManagedSql().setScripts(new ArrayList<>());
    expected.getSpec().getManagedSql().getScripts().add(new StackGresClusterManagedScriptEntry());
    expected.getSpec().getManagedSql().getScripts().get(0)
        .setId(0);
    expected.getSpec().getManagedSql().getScripts().get(0)
        .setSgScript(ManagedSqlUtil.defaultName(expected));
    expected.getStatus().getManagedSql().setScripts(new ArrayList<>());
    expected.getStatus().getManagedSql().getScripts()
        .add(new StackGresClusterManagedScriptEntryStatus());
    expected.getStatus().getManagedSql().getScripts().get(0)
        .setId(0);
    JsonNode expectedCluster = JsonUtil.toJson(expected);

    review.getRequest().getObject().setStatus(null);

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expectedCluster,
        JsonUtil.toJson(result));
  }

  @Test
  void createClusterWithScriptIdAndNoStatus_shouldAddStatusWithDefaultScript() {
    StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadCreateWithManagedSql().get();

    review.getRequest().getObject().getSpec().getManagedSql().setScripts(new ArrayList<>());
    review.getRequest().getObject().getSpec().getManagedSql().getScripts()
        .add(new StackGresClusterManagedScriptEntry());
    review.getRequest().getObject().getSpec().getManagedSql().getScripts().get(0)
        .setId(0);
    review.getRequest().getObject().getSpec().getManagedSql().getScripts().get(0)
        .setSgScript("test");
    review.getRequest().getObject().getStatus().getManagedSql().setScripts(null);

    StackGresCluster expected = JsonUtil.copy(review.getRequest().getObject());
    expected.getSpec().getManagedSql().getScripts().add(new StackGresClusterManagedScriptEntry());
    expected.getSpec().getManagedSql().getScripts().get(1)
        .setId(1);
    expected.getSpec().getManagedSql().getScripts().get(1)
        .setSgScript(ManagedSqlUtil.defaultName(expected));
    expected.getStatus().getManagedSql().setScripts(new ArrayList<>());
    expected.getStatus().getManagedSql().getScripts()
        .add(new StackGresClusterManagedScriptEntryStatus());
    expected.getStatus().getManagedSql().getScripts().get(0)
        .setId(0);
    expected.getStatus().getManagedSql().getScripts()
        .add(new StackGresClusterManagedScriptEntryStatus());
    expected.getStatus().getManagedSql().getScripts().get(1)
        .setId(1);
    JsonNode expectedCluster = JsonUtil.toJson(expected);

    review.getRequest().getObject().setStatus(null);

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expectedCluster,
        JsonUtil.toJson(result));
  }

  @Test
  void createClusterWithouIds_shouldAddThem() {
    StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadCreateWithManagedSql().get();
    review.getRequest().getObject().getStatus().getManagedSql().getScripts().get(0)
        .setScripts(null);
    review.getRequest().getObject().getStatus().getManagedSql().getScripts().get(1)
        .setScripts(null);
    review.getRequest().getObject().getStatus().getManagedSql().getScripts().get(2)
        .setScripts(null);
    review.getRequest().getObject().getStatus().getManagedSql().getScripts().get(3)
        .setScripts(null);
    final JsonNode expectedCluster = JsonUtil.toJson(review.getRequest().getObject());

    review.getRequest().getObject().getSpec().getManagedSql().getScripts().stream()
        .forEach(scriptEntry -> scriptEntry.setId(null));
    review.getRequest().getObject().getStatus().setManagedSql(
        new StackGresClusterManagedSqlStatus());

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expectedCluster,
        JsonUtil.toJson(result));
  }

  @Test
  void updateClusterWithWithoutModification_shouldDoNothing() {
    StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadUpdateWithManagedSql().get();

    JsonNode expectedCluster = JsonUtil.toJson(review.getRequest().getObject());

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expectedCluster,
        JsonUtil.toJson(result));
  }

  @Test
  void updateClusterWithNoScripts_shouldAddDefaultScript() {
    StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadUpdateWithManagedSql().get();

    review.getRequest().getObject().getSpec().getManagedSql().setScripts(
        null);
    StackGresCluster expected = JsonUtil.copy(review.getRequest().getObject());
    expected.getSpec().getManagedSql().setScripts(new ArrayList<>());
    expected.getSpec().getManagedSql().getScripts().add(new StackGresClusterManagedScriptEntry());
    expected.getSpec().getManagedSql().getScripts().get(0)
        .setId(0);
    expected.getSpec().getManagedSql().getScripts().get(0)
        .setSgScript(ManagedSqlUtil.defaultName(expected));
    expected.getStatus().getManagedSql().setScripts(new ArrayList<>());
    expected.getStatus().getManagedSql().getScripts()
        .add(new StackGresClusterManagedScriptEntryStatus());
    expected.getStatus().getManagedSql().getScripts().get(0)
        .setId(0);
    JsonNode expectedCluster = JsonUtil.toJson(expected);

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expectedCluster,
        JsonUtil.toJson(result));
  }

  @Test
  void updateClusterWithoutStatus_shouldAddDefaultScriptAndStatus() {
    StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadUpdateWithManagedSql().get();

    review.getRequest().getObject().getSpec().getManagedSql().setScripts(null);
    review.getRequest().getObject().getStatus().getManagedSql().setScripts(null);
    StackGresCluster expected = JsonUtil.copy(review.getRequest().getObject());
    expected.getSpec().getManagedSql().setScripts(new ArrayList<>());
    expected.getSpec().getManagedSql().getScripts().add(new StackGresClusterManagedScriptEntry());
    expected.getSpec().getManagedSql().getScripts().get(0)
        .setId(0);
    expected.getSpec().getManagedSql().getScripts().get(0)
        .setSgScript(ManagedSqlUtil.defaultName(expected));
    expected.getStatus().getManagedSql().setScripts(new ArrayList<>());
    expected.getStatus().getManagedSql().getScripts()
        .add(new StackGresClusterManagedScriptEntryStatus());
    expected.getStatus().getManagedSql().getScripts().get(0)
        .setId(0);
    JsonNode expectedCluster = JsonUtil.toJson(expected);

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expectedCluster,
        JsonUtil.toJson(result));
  }

  @Test
  void updateClusterAddingAnEntry_shouldSetIdAndVersion() {
    StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadUpdateWithManagedSql().get();

    review.getRequest().getObject().getSpec().getManagedSql().getScripts()
        .add(1, new StackGresClusterManagedScriptEntry());

    StackGresCluster expected = JsonUtil.copy(review.getRequest().getObject());
    expected.getSpec().getManagedSql().getScripts().get(1).setId(4);
    expected.getStatus().getManagedSql().getScripts()
        .add(new StackGresClusterManagedScriptEntryStatus());
    expected.getStatus().getManagedSql().getScripts().get(4).setId(4);
    JsonNode expectedCluster = JsonUtil.toJson(expected);

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expectedCluster,
        JsonUtil.toJson(result));
  }

  @Test
  void updateClusterRemovingAnEntry_shouldRemoveItsStatus() {
    StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadUpdateWithManagedSql().get();

    review.getRequest().getObject().getSpec().getManagedSql().getScripts().remove(1);
    StackGresCluster expected = JsonUtil.copy(review.getRequest().getObject());
    expected.getStatus().getManagedSql().getScripts().remove(1);
    JsonNode expectedCluster = JsonUtil.toJson(expected);

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expectedCluster,
        JsonUtil.toJson(result));
  }

  @Test
  void updateClusterWithDefaultRemovedWithDifferentId_shouldAddDefaultAndResetStatus() {
    StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadUpdateWithManagedSql().get();

    review.getRequest().getObject().getStatus().getManagedSql().getScripts().get(0).setId(4);
    StackGresCluster expected = JsonUtil.copy(review.getRequest().getObject());
    review.getRequest().getObject().getSpec().getManagedSql().getScripts().remove(0);
    expected.getSpec().getManagedSql().getScripts().get(0).setId(4);
    expected.getStatus().getManagedSql().getScripts().get(0).setId(4);
    JsonNode expectedCluster = JsonUtil.toJson(expected);

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expectedCluster,
        JsonUtil.toJson(result));
  }

}

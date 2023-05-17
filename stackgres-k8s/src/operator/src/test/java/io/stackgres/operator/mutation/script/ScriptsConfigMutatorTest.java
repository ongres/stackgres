/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.script;

import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import io.stackgres.common.crd.sgscript.StackGresScriptEntryStatus;
import io.stackgres.common.crd.sgscript.StackGresScriptStatus;
import io.stackgres.operator.common.StackGresScriptReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScriptsConfigMutatorTest {

  protected static final ObjectMapper JSON_MAPPER = new ObjectMapper();

  private ScriptsConfigMutator mutator = new ScriptsConfigMutator();

  @Test
  void createScriptAlreadyValid_shouldDoNothing() {
    StackGresScriptReview review = AdmissionReviewFixtures.script().loadCreate().get();

    JsonNode expectedScript = JsonUtil.toJson(review.getRequest().getObject());

    StackGresScript result = mutator.mutate(review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        JsonUtil.toJson(expectedScript),
        JsonUtil.toJson(result));
  }

  @Test
  void createScriptWithNullManagedVersions_shouldSetItToTrue() {
    StackGresScriptReview review = AdmissionReviewFixtures.script().loadCreate().get();

    JsonNode expectedScript = JsonUtil.toJson(review.getRequest().getObject());

    review.getRequest().getObject().getSpec().setManagedVersions(null);

    StackGresScript result = mutator.mutate(review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expectedScript,
        JsonUtil.toJson(result));
  }

  @Test
  void createScriptWithFalseManagedVersions_shouldDoNothing() {
    StackGresScriptReview review = AdmissionReviewFixtures.script().loadCreate().get();
    review.getRequest().getObject().getSpec().setManagedVersions(false);

    JsonNode expectedScript = JsonUtil.toJson(review.getRequest().getObject());

    StackGresScript result = mutator.mutate(review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expectedScript,
        JsonUtil.toJson(result));
  }

  @Test
  void createScriptWithoutScriptsAndScriptsStatuses_shouldDoNothing() {
    StackGresScriptReview review = AdmissionReviewFixtures.script()
        .loadCreate().get();
    review.getRequest().getObject().getSpec().setScripts(null);
    review.getRequest().getObject().getStatus().setScripts(new ArrayList<>());

    JsonNode expectedScript = JsonUtil.toJson(review.getRequest().getObject());

    StackGresScript result = mutator.mutate(review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expectedScript,
        JsonUtil.toJson(result));
  }

  @Test
  void createScriptWithoutScriptsAndStatus_shouldDoNothing() {
    StackGresScriptReview review = AdmissionReviewFixtures.script()
        .loadCreate().get();
    review.getRequest().getObject().getSpec().setScripts(null);
    review.getRequest().getObject().setStatus(new StackGresScriptStatus());

    JsonNode expectedScript = JsonUtil.toJson(review.getRequest().getObject());

    StackGresScript result = mutator.mutate(review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expectedScript,
        JsonUtil.toJson(result));
  }

  @Test
  void createScriptWithouIds_shouldAddThem() {
    StackGresScriptReview review = AdmissionReviewFixtures.script()
        .loadCreate().get();

    final JsonNode expectedScript = JsonUtil.toJson(review.getRequest().getObject());

    review.getRequest().getObject().getSpec().getScripts().stream()
        .forEach(scriptEntry -> scriptEntry.setId(null));
    review.getRequest().getObject().setStatus(new StackGresScriptStatus());

    StackGresScript result = mutator.mutate(review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expectedScript,
        JsonUtil.toJson(result));
  }

  @Test
  void updateScriptWithoutModification_shouldDoNothing() {
    StackGresScriptReview review = AdmissionReviewFixtures.script()
        .loadUpdate().get();

    JsonNode expectedScript = JsonUtil.toJson(review.getRequest().getObject());

    StackGresScript result = mutator.mutate(review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expectedScript,
        JsonUtil.toJson(result));
  }

  @Test
  void updateScriptRemovingScripts_shouldRemoveTheScriptsStatuses() {
    StackGresScriptReview review = AdmissionReviewFixtures.script()
        .loadUpdate().get();
    review.getRequest().getObject().getSpec().setScripts(null);
    review.getRequest().getOldObject().getSpec().setScripts(null);

    StackGresScript expected = JsonUtil.copy(review.getRequest().getObject());
    expected.getStatus().setScripts(new ArrayList<>());
    JsonNode expectedScript = JsonUtil.toJson(expected);

    StackGresScript result = mutator.mutate(review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expectedScript,
        JsonUtil.toJson(result));
  }

  @Test
  void updateScriptWithoutStatusAndScriptsStatuses_shouldDoNothing() {
    StackGresScriptReview review = AdmissionReviewFixtures.script()
        .loadUpdate().get();
    review.getRequest().getObject().getSpec().setScripts(null);
    review.getRequest().getObject().getStatus().setScripts(new ArrayList<>());
    review.getRequest().getOldObject().getSpec().setScripts(null);

    JsonNode expectedScript = JsonUtil.toJson(review.getRequest().getObject());

    StackGresScript result = mutator.mutate(review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expectedScript,
        JsonUtil.toJson(result));
  }

  @Test
  void updateScriptAddingAnEntry_shouldSetIdAndVersion() {
    StackGresScriptReview review = AdmissionReviewFixtures.script()
        .loadUpdate().get();
    review.getRequest().getObject().getSpec().getScripts().add(1, new StackGresScriptEntry());

    StackGresScript expected = JsonUtil.copy(review.getRequest().getObject());
    expected.getSpec().getScripts().get(1).setId(3);
    expected.getSpec().getScripts().get(1).setVersion(0);
    expected.getStatus().getScripts().add(1, new StackGresScriptEntryStatus());
    expected.getStatus().getScripts().get(1).setId(3);
    JsonNode expectedScript = JsonUtil.toJson(expected);

    StackGresScript result = mutator.mutate(review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expectedScript,
        JsonUtil.toJson(result));
  }

  @Test
  void updateScriptRemovingAnEntryAndItsStatus_shouldDoNothing() {
    StackGresScriptReview review = AdmissionReviewFixtures.script()
        .loadUpdate().get();
    review.getRequest().getObject().getSpec().getScripts().remove(1);
    review.getRequest().getObject().getStatus().getScripts().remove(1);

    JsonNode expectedScript = JsonUtil.toJson(review.getRequest().getObject());

    StackGresScript result = mutator.mutate(review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expectedScript,
        JsonUtil.toJson(result));
  }

}

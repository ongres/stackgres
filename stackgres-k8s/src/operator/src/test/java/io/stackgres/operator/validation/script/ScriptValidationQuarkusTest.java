/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.script;

import static io.stackgres.testutil.JsonUtil.readFromJson;
import static org.hamcrest.Matchers.is;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.stackgres.operator.common.StackGresScriptReview;
import io.stackgres.operator.validation.ValidationUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@QuarkusTest
@TestInstance(Lifecycle.PER_CLASS)
class ScriptValidationQuarkusTest {

  @Inject
  KubernetesClient client;

  @BeforeAll
  void setUp() {
    StackGresScriptReview review = getConstraintScriptReview();
    review.getRequest().getObject().getSpec().getScripts().stream()
        .filter(script -> script.getScriptFrom() != null
            && script.getScriptFrom().getConfigMapKeyRef() != null)
        .map(script -> script.getScriptFrom().getConfigMapKeyRef())
        .forEach(configMapKeyRef -> {
          ConfigMap configMap = new ConfigMapBuilder()
              .withNewMetadata()
              .withNamespace(review.getRequest().getObject().getMetadata().getNamespace())
              .withName(configMapKeyRef.getName())
              .endMetadata()
              .withData(ImmutableMap.of(
                  configMapKeyRef.getKey(), "SELECT 1"))
              .build();
          client.configMaps().create(configMap);
        });
    review.getRequest().getObject().getSpec().getScripts().stream()
        .filter(script -> script.getScriptFrom() != null
            && script.getScriptFrom().getSecretKeyRef() != null)
        .map(script -> script.getScriptFrom().getSecretKeyRef())
        .forEach(configMapKeyRef -> {
          Secret configMap = new SecretBuilder()
              .withNewMetadata()
              .withNamespace(review.getRequest().getObject().getMetadata().getNamespace())
              .withName(configMapKeyRef.getName())
              .endMetadata()
              .withData(ImmutableMap.of(
                  configMapKeyRef.getKey(), "SELECT 1"))
              .build();
          client.secrets().create(configMap);
        });
  }

  private StackGresScriptReview getConstraintScriptReview() {
    var review = readFromJson("script_allow_requests/valid_creation.json",
        StackGresScriptReview.class);
    review.getRequest().getObject().getMetadata().setNamespace("test");
    return review;
  }

  @Test
  void given_validStackGresScriptReview_shouldNotFail() {
    StackGresScriptReview clusterReview = getConstraintScriptReview();
    RestAssured.given()
        .body(clusterReview)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post(ValidationUtil.SCRIPT_VALIDATION_PATH)
        .then()
        .body("response.allowed", is(true),
            "kind", is("AdmissionReview"))
        .statusCode(200);
  }

}

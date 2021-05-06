/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.StringUtil;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@QuarkusTest
@EnabledIfEnvironmentVariable(named = "QUARKUS_PROFILE", matches = "test")
class SecretResourceTest implements AuthenticatedResourceTest {

  @Inject
  KubernetesClientFactory factory;

  @BeforeEach
  void setUp() {
    try(KubernetesClient client = factory.create()){
      client.secrets().inNamespace("test").delete();
    }
  }

  @Test
  void ifNoSecretsAreCreated_itShouldReturnAnEmptyArray() {
    given()
        .header(AUTHENTICATION_HEADER)
        .get("/stackgres/secrets/test")
        .then().statusCode(200)
        .body("", Matchers.hasSize(0));
  }

  @Test
  void ifSecretsAreCreated_itShouldReturnThenInAnArray() {

    final String randomPlainValue = StringUtil.generateRandom();

    try (KubernetesClient client = factory.create()) {
      client.secrets().inNamespace("test")
          .create(new SecretBuilder()
              .withData(ImmutableMap.of("testKey", randomPlainValue))
              .withNewMetadata()
              .withName("testSecret")
              .endMetadata()
              .build());
    }

    given()
        .when()
        .header(AUTHENTICATION_HEADER)
        .get("/stackgres/secrets/test")
        .then().statusCode(200)
        .body("", Matchers.hasSize(1))
        .body("[0].keys", Matchers.contains("testKey"))
        .body("[0].metadata.name", is("testSecret"));

  }
}
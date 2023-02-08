/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.StringUtil;
import io.stackgres.testutil.StackGresKubernetesMockServerSetup;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@WithKubernetesTestServer(https = true, setup = StackGresKubernetesMockServerSetup.class)
class SecretResourceTest implements AuthenticatedResourceTest {

  @KubernetesTestServer
  KubernetesServer mockServer;

  @BeforeEach
  void setUp() {
    mockServer.getClient().secrets().inNamespace("test").delete();
  }

  @Test
  void ifNoSecretsAreCreated_itShouldReturnAnEmptyArray() {
    given()
        .header(AUTHENTICATION_HEADER)
        .get("/stackgres/namespaces/test/secrets")
        .then().statusCode(200)
        .body("", Matchers.hasSize(0));
  }

  @Test
  void ifSecretsAreCreated_itShouldReturnThenInAnArray() {

    final String randomPlainValue = StringUtil.generateRandom();

    mockServer.getClient().secrets().inNamespace("test")
        .resource(new SecretBuilder()
            .withData(ImmutableMap.of("testKey", randomPlainValue))
            .withNewMetadata()
            .withName("testSecret")
            .endMetadata()
            .build())
        .create();

    given()
        .when()
        .header(AUTHENTICATION_HEADER)
        .get("/stackgres/namespaces/test/secrets")
        .then().statusCode(200)
        .body("", Matchers.hasSize(1))
        .body("[0].keys", Matchers.contains("testKey"))
        .body("[0].metadata.name", is("testSecret"));

  }
}

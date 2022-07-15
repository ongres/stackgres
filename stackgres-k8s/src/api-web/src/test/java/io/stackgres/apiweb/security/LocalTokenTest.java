/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.security;

import static io.restassured.RestAssured.given;

import java.util.Map;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.restassured.http.ContentType;
import io.stackgres.apiweb.config.WebApiProperty;
import io.stackgres.common.StackGresContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import io.stackgres.testutil.StackGresKubernetesMockServerSetup;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@WithKubernetesTestServer(https = true, setup = StackGresKubernetesMockServerSetup.class)
class LocalTokenTest {

  @KubernetesTestServer
  KubernetesServer mockServer;

  String namespace = WebApiProperty.RESTAPI_NAMESPACE.getString();

  @BeforeEach
  void setupSecret() {
    Secret demoUser = new SecretBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName("demo-user")
        .withLabels(Map.of(StackGresContext.AUTH_KEY, StackGresContext.AUTH_USER_VALUE))
        .endMetadata()
        .withType("Opaque")
        .withData(Map.of(
            StackGresContext.REST_APIUSER_KEY, ResourceUtil.encodeSecret("apiuser"),
            StackGresContext.REST_K8SUSER_KEY, ResourceUtil.encodeSecret("stackgres"),
            StackGresContext.REST_PASSWORD_KEY,
            ResourceUtil.encodeSecret(TokenUtils.sha256("apiuserdemo123"))))
        .build();
    Secret demoUserNoLabels = new SecretBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName("demo-user-no-labels")
        .endMetadata()
        .withType("Opaque")
        .withData(Map.of(
            StackGresContext.REST_APIUSER_KEY, ResourceUtil.encodeSecret("apiuser-nolabel"),
            StackGresContext.REST_K8SUSER_KEY, ResourceUtil.encodeSecret("apiuser-nolabel"),
            StackGresContext.REST_PASSWORD_KEY,
            ResourceUtil.encodeSecret(TokenUtils.sha256("apiuser-nolabeldemo123"))))
        .build();
    Secret demoUserNoApiUser = new SecretBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName("demo-user-no-apiuser")
        .withLabels(Map.of(StackGresContext.AUTH_KEY, StackGresContext.AUTH_USER_VALUE))
        .endMetadata()
        .withType("Opaque")
        .withData(Map.of(
            StackGresContext.REST_K8SUSER_KEY, ResourceUtil.encodeSecret("k8suser"),
            StackGresContext.REST_PASSWORD_KEY,
            ResourceUtil.encodeSecret(TokenUtils.sha256("k8suserdemo123"))))
        .build();

    mockServer.getClient().secrets().inNamespace(namespace)
        .createOrReplace(demoUser);
    mockServer.getClient().secrets().inNamespace(namespace)
        .createOrReplace(demoUserNoLabels);
    mockServer.getClient().secrets().inNamespace(namespace)
        .createOrReplace(demoUserNoApiUser);
  }

  @AfterEach
  void dropSecret() {
    mockServer.getClient().secrets().inNamespace(namespace)
        .withName("demo-user").delete();
    mockServer.getClient().secrets().inNamespace(namespace)
        .withName("demo-user-no-labels").delete();
    mockServer.getClient().secrets().inNamespace(namespace)
        .withName("demo-user-no-apiuser").delete();
  }

  @Test
  void try_login_success_apiuser() {
    UserPassword up = new UserPassword("apiuser", "demo123");

    given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .body(up)
        .when()
        .post("/stackgres/auth/login")
        .then()
        .statusCode(200)
        .body("access_token", Matchers.startsWith("eyJ"),
            "token_type", Matchers.is("Bearer"),
            "expires_in", Matchers.is(28800));
  }

  @Test
  void try_login_success_k8suser() {
    UserPassword up = new UserPassword("k8suser", "demo123");

    given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .body(up)
        .when()
        .post("/stackgres/auth/login")
        .then()
        .statusCode(200)
        .body("access_token", Matchers.startsWith("eyJ"),
            "token_type", Matchers.is("Bearer"),
            "expires_in", Matchers.is(28800));
  }

  @Test
  void try_login_failure_no_label() {
    UserPassword up = new UserPassword("apiuser-nolabel", "demo123");

    given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .body(up)
        .when()
        .post("/stackgres/auth/login")
        .then()
        .statusCode(403);
  }

  @Test
  void try_login_failure_bad_passwd() {
    UserPassword up = new UserPassword("apiuser", "123");

    given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .body(up)
        .when()
        .post("/stackgres/auth/login")
        .then()
        .statusCode(403);
  }

  @Test
  void get_local_jwt_redirect() {
    String generateTokenString = TokenUtils.generateTokenString("admin", "stackgres");
    given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .auth().oauth2(generateTokenString)
        .queryParam("redirectTo", "http://localhost:8081/stackgres/version/postgresql")
        .when()
        .get("/stackgres/auth/external")
        .then()
        .statusCode(200);
  }

  @Test
  void get_local_jwt_redirect_no_auth() {
    given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .queryParam("redirectTo", "http://localhost:8081/stackgres/version/postgresql")
        .when()
        .get("/stackgres/auth/external")
        .then()
        .statusCode(401)
        .header("WWW-Authenticate", "Bearer");
  }

}

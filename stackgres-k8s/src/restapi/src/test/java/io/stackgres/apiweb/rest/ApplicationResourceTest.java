/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

import java.nio.file.Path;
import java.nio.file.Paths;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.KubernetesTestServerSetup;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@WithKubernetesTestServer(setup = KubernetesTestServerSetup.class)
@QuarkusTest
class ApplicationResourceTest implements AuthenticatedResourceTest {

  @Test
  void getApplicationEndpointWithoutAuth_shouldFail() {
    given()
        .auth().none()
        .get("/stackgres/applications")
        .then()
        .statusCode(401);
  }

  @Test
  void getApplicationEndpoint_shouldReturnBabelfishCompass() {
    given()
        .auth().oauth2(AUTH_TOKEN)
        .get("/stackgres/applications")
        .then()
        .body("applications", hasSize(1))
        .body("applications.name", hasItems("babelfish-compass"))
        .body("applications.publisher", hasItems("com.ongres"))
        .statusCode(200);
  }

  @Test
  void getApplicationBabelfishEndpoint_shouldNotFail() {
    given()
        .auth().oauth2(AUTH_TOKEN)
        .pathParam("publisher", "com.ongres")
        .pathParam("name", "babelfish-compass")
        .when()
        .get("/stackgres/applications/{publisher}/{name}")
        .then()
        .body("publisher", equalTo("com.ongres"))
        .body("name", equalTo("babelfish-compass"))
        .statusCode(200);
  }

  @Test
  @Disabled("This is used only for local testing")
  void whenBabelfishUploadSql_shouldNotFail() {
    Path resourceDir = Paths.get("src", "test", "resources", "t-sql");
    Path tsql1 = resourceDir.resolve("tsql-example.sql").toAbsolutePath();
    Path tsql2 = resourceDir.resolve("Sales.sql").toAbsolutePath();

    given()
        .auth().oauth2(AUTH_TOKEN)
        .pathParam("publisher", "com.ongres")
        .pathParam("name", "babelfish-compass")
        .multiPart("reportName", "MyReport")
        .multiPart("sqlFiles", tsql1.toFile())
        .multiPart("sqlFiles", tsql2.toFile())
        .when()
        .post("/stackgres/applications/{publisher}/{name}")
        .then()
        .statusCode(200);
  }

}

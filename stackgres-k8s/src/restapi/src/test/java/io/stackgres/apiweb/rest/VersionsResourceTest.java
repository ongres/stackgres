/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.KubernetesTestServerSetup;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.docir.StackGresContextMock;
import io.stackgres.common.fixture.Fixtures;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

@WithKubernetesTestServer(setup = KubernetesTestServerSetup.class)
@QuarkusTest
class VersionsResourceTest implements AuthenticatedResourceTest {

  @Test
  void get_listOf_postgresql_versions() {
    String[] pgvers = StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
        .streamOrderedVersions(StackGresContextMock.CONTEXT)
        .toArray(String[]::new);

    given()
        .header(AUTHENTICATION_HEADER)
        .when()
        .get("/stackgres/version/postgresql")
        .then()
        .statusCode(200)
        .body("postgresql", Matchers.hasItems(pgvers));
  }

  @Test
  void get_listOf_babelfish_versions() {
    String[] pgvers = StackGresComponent.BABELFISH.get(Fixtures.registryCluster())
        .streamOrderedVersions(StackGresContextMock.CONTEXT)
        .toArray(String[]::new);

    given()
        .header(AUTHENTICATION_HEADER)
        .when()
        .get("/stackgres/version/postgresql?flavor=babelfish")
        .then()
        .statusCode(200)
        .body("postgresql", Matchers.hasItems(pgvers));
  }

}

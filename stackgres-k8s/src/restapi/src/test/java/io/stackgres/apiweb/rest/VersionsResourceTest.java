/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.StackGresComponent;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

@QuarkusTest
class VersionsResourceTest implements AuthenticatedResourceTest {

  @Test
  void get_listOf_postgresql_versions() {
    String[] pgvers = StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions()
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
    String[] pgvers = StackGresComponent.BABELFISH.getLatest().streamOrderedVersions()
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

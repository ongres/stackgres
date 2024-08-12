/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(ApplicationResourceDisabledProfileTest.DisableBbfCompass.class)
class ApplicationResourceDisabledProfileTest implements AuthenticatedResourceTest {

  @Test
  void givenApplicationsNotEnabled_shouldFail() {
    given()
        .auth().oauth2(AUTH_TOKEN)
        .when()
        .get("/stackgres/applications")
        .then()
        .body("applications", hasSize(0))
        .statusCode(200);
  }

  @Test
  void givenBbfCompassNotEnabled_shouldFail() {
    given()
        .auth().oauth2(AUTH_TOKEN)
        .pathParam("publisher", "com.ongres")
        .pathParam("name", "babelfish-compass")
        .when()
        .get("/stackgres/applications/{publisher}/{name}")
        .then()
        .statusCode(404);
  }

  public static class DisableBbfCompass implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.of("stackgres.applications.babelfish-compass.enabled", "false");
    }
  }

}

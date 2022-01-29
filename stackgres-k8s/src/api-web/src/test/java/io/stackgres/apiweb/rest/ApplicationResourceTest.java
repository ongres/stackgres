/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static io.restassured.RestAssured.given;
import static org.mockito.Mockito.when;

import java.util.List;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import io.stackgres.apiweb.dto.ApplicationDto;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ApplicationResourceTest implements AuthenticatedResourceTest {

  @InjectMock
  ApplicationsResource resource;

  @Test
  void getApplicationEndpoint_shouldNotFail() {
    given()
        .header(AUTHENTICATION_HEADER)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .get("/stackgres/application")
        .then()
        .statusCode(200);
  }

  @Test
  void getApplicationEndpoint_shouldReturnBabelfishCompass() {
    when(resource.getAllApplications())
        .thenReturn(List.of(new ApplicationDto.Builder()
            .name("babelfish-compass")
            .publisher("com.ongres")
            .build()));

    given()
        .header(AUTHENTICATION_HEADER)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .get("/stackgres/application")
        .then()
        .body(".", Matchers.hasSize(1))
        .body("[0].name", Matchers.equalTo("babelfish-compass"))
        .body("[0].publisher", Matchers.equalTo("com.ongres"))
        .statusCode(200);
  }

}

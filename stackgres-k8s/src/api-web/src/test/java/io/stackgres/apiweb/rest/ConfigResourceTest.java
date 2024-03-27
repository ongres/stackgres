/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.restassured.http.ContentType;
import io.stackgres.apiweb.dto.config.ConfigDto;
import io.stackgres.apiweb.transformer.ConfigTransformerTest;
import io.stackgres.common.StackGresKubernetesMockServerSetup;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.resource.ConfigFinder;
import io.stackgres.common.resource.ConfigScanner;
import io.stackgres.common.resource.ConfigScheduler;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

@QuarkusTest
@WithKubernetesTestServer(https = true, setup = StackGresKubernetesMockServerSetup.class)
class ConfigResourceTest implements AuthenticatedResourceTest {

  @InjectMock
  ConfigScheduler scheduler;

  @InjectMock
  ConfigFinder finder;

  @InjectMock
  ConfigScanner scanner;

  @Test
  @DisplayName("Given a created list of object resources it should list them")
  void testConfigList() {
    var configTuple = ConfigTransformerTest.createConfig();
    doReturn(
        List.of(configTuple.source())
    ).when(scanner).getResources();

    var response = given()
        .header(AUTHENTICATION_HEADER)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .get("/stackgres/sgconfigs")
        .then()
        .statusCode(200)
        .extract()
        .as(ConfigDto[].class);

    JsonUtil.assertJsonEquals(
        JsonUtil.toJson(List.of(configTuple.target())),
        JsonUtil.toJson(Arrays.asList(response))
    );
  }

  @Test
  @DisplayName("The config creation should not fail")
  void testConfigCreation() {
    var configTuple = ConfigTransformerTest
        .createConfig();

    when(scheduler.create(any(), anyBoolean()))
        .thenAnswer(invocation -> invocation.getArgument(0, StackGresConfig.class));

    given().header(AUTHENTICATION_HEADER)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(configTuple.target())
        .post("/stackgres/sgconfigs")
        .then()
        .statusCode(200);

    verify(scheduler).create(any(), anyBoolean());
  }

  @Test
  @DisplayName("The object storage update should not fail")
  void testConfigUpdate() {
    var configTuple = ConfigTransformerTest
        .createConfig();

    String namespace = configTuple.source().getMetadata().getNamespace();
    String name = configTuple.source().getMetadata().getName();

    when(finder.findByNameAndNamespace(name, namespace)).thenReturn(
        Optional.of(configTuple.source())
    );

    when(scheduler.update(any(), any()))
        .then(
            (Answer<StackGresConfig>) invocationOnMock -> invocationOnMock
                .getArgument(0, StackGresConfig.class)
        );

    given().header(AUTHENTICATION_HEADER)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(configTuple.target())
        .put("/stackgres/sgconfigs")
        .then()
        .statusCode(200);

    verify(scheduler).update(any(), any());
  }

  @Test
  @DisplayName("The object storage dalete should not fail")
  void testConfigDeletion() {
    var configTuple = ConfigTransformerTest
        .createConfig();

    String namespace = configTuple.source().getMetadata().getNamespace();
    String name = configTuple.source().getMetadata().getName();

    when(finder.findByNameAndNamespace(name, namespace)).thenReturn(
        Optional.of(configTuple.source())
    );

    doNothing().when(scheduler).delete(any());

    given().header(AUTHENTICATION_HEADER)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(configTuple.target())
        .delete("/stackgres/sgconfigs")
        .then()
        .statusCode(204);

    verify(scheduler).delete(any(), anyBoolean());
  }
}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static io.restassured.RestAssured.given;

import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.stackgres.apiweb.dto.fixture.DtoFixtures;
import io.stackgres.apiweb.dto.pooling.PoolingConfigDto;
import io.stackgres.common.StackGresKubernetesMockServerSetup;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigList;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.testutil.StringUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@WithKubernetesTestServer(https = true, setup = StackGresKubernetesMockServerSetup.class)
class PgBouncerConfigResourceQuarkusTest implements AuthenticatedResourceTest {

  @KubernetesTestServer
  KubernetesServer mockServer;

  private StackGresPoolingConfig resource;

  @BeforeEach
  void setUp() {
    StackGresPoolingConfig customResource = getCustomResource();
    customResource.getMetadata().setNamespace(StringUtils.getRandomNamespace());
    customResource.getMetadata().setName(StringUtils.getRandomClusterName());
    customResource.getMetadata().setSelfLink(null);
    this.resource = mockServer.getClient().resources(
        StackGresPoolingConfig.class,
        StackGresPoolingConfigList.class)
        .inNamespace(customResource.getMetadata().getNamespace())
        .resource(customResource)
        .create();
  }

  @AfterEach
  void tearDown() {
    mockServer.getClient().resources(
        StackGresPoolingConfig.class,
        StackGresPoolingConfigList.class)
        .inNamespace(resource.getMetadata().getNamespace())
        .withName(resource.getMetadata().getName())
        .delete();
  }

  private PoolingConfigDto getDto() {
    return DtoFixtures.poolingConfig().loadDefault().get();
  }

  private StackGresPoolingConfig getCustomResource() {
    return Fixtures.poolingConfig().loadDefault().get();
  }

  @Test
  void postCreation_ShouldNotFail() {
    PoolingConfigDto dto = getDto();
    dto.getMetadata().setNamespace("test");

    given()
        .header(AUTHENTICATION_HEADER)
        .body(dto)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post("/stackgres/sgpoolconfigs")
        .then()
        .statusCode(200);
  }

  @Test
  void getListEndpoint_ShouldReturnPgBouncerIniParameters() {
    given()
        .header(AUTHENTICATION_HEADER)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .get("/stackgres/sgpoolconfigs")
        .then()
        .statusCode(200)
        .body("find { it.metadata.name == '%s' }.spec.pgBouncer.'pgbouncer.ini'",
            RestAssured.withArgs(resource.getMetadata().getName()),
            Matchers.equalTo("""
                [databases]
                foodb = dbname=bardb pool_size=10
                sgdb = pool_mode=statement max_db_connections=1000

                [users]
                user1 = max_user_connections=30
                user2 = pool_mode=session max_user_connections=100

                [pgbouncer]
                default_pool_size = 200
                max_client_conn = 200
                pool_mode = transaction

                """));
  }

  @Test
  void getNamedEndpoint_ShouldReturnPgBouncerIniParameters() {
    given()
        .header(AUTHENTICATION_HEADER)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .pathParam("namespace", resource.getMetadata().getNamespace())
        .pathParam("name", resource.getMetadata().getName())
        .get("/stackgres/namespaces/{namespace}/sgpoolconfigs/{name}")
        .then()
        .statusCode(200)
        .body("spec.pgBouncer.'pgbouncer.ini'",
            RestAssured.withArgs(resource.getMetadata().getName()),
            Matchers.equalTo("""
                [databases]
                foodb = dbname=bardb pool_size=10
                sgdb = pool_mode=statement max_db_connections=1000

                [users]
                user1 = max_user_connections=30
                user2 = pool_mode=session max_user_connections=100

                [pgbouncer]
                default_pool_size = 200
                max_client_conn = 200
                pool_mode = transaction

                """));
  }

  @Test
  void putEndpoint_ShouldUpdateConfig() {
    PoolingConfigDto dto = getDto();
    dto.getMetadata().setNamespace("demo");
    dto.getMetadata().setName("test");

    given()
        .header(AUTHENTICATION_HEADER)
        .body(dto)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post("/stackgres/sgpoolconfigs")
        .then()
        .statusCode(200);

    dto.getSpec().getPgBouncer()
        .setParameters("""
            [databases]
            bardb = dbname=foodb pool_size=100
            sgdb = pool_mode=statement max_db_connections=500

            [users]
            user1 = max_user_connections=30 pool_mode=transaction
            user2 = pool_mode=session max_user_connections=50

            [pgbouncer]
            default_pool_size = 300
            max_client_conn = 400
            pool_mode = transaction

            """);

    given()
        .header(AUTHENTICATION_HEADER)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(dto)
        .put("/stackgres/sgpoolconfigs")
        .then()
        .statusCode(200);
  }

}

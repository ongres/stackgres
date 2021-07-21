/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static io.restassured.RestAssured.given;

import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.stackgres.apiweb.dto.pooling.PoolingConfigDto;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigList;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.StringUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PgBouncerConfigResourceQuarkusTest implements AuthenticatedResourceTest {

  @Inject
  KubernetesClientFactory factory;

  @Inject
  ObjectMapper mapper;

  private StackGresPoolingConfig resource;

  @BeforeEach
  void setUp() {
    StackGresPoolingConfig customResource = getCustomResource();
    customResource.getMetadata().setNamespace(StringUtils.getRandomNamespace());
    customResource.getMetadata().setName(StringUtils.getRandomClusterName());
    customResource.getMetadata().setSelfLink(null);
    try (KubernetesClient client = factory.create()) {
      this.resource = client.customResources(
          StackGresPoolingConfig.class,
          StackGresPoolingConfigList.class)
          .inNamespace(customResource.getMetadata().getNamespace())
          .withName(customResource.getMetadata().getNamespace())
          .create(customResource);
    }
  }

  @AfterEach
  void tearDown() {
    try (KubernetesClient client = factory.create()) {
      client.customResources(
          StackGresPoolingConfig.class,
          StackGresPoolingConfigList.class)
          .inNamespace(resource.getMetadata().getNamespace())
          .withName(resource.getMetadata().getName())
          .delete();
    }
  }

  private PoolingConfigDto getDto() {
    return JsonUtil.readFromJson("pooling_config/dto.json", PoolingConfigDto.class);
  }

  private StackGresPoolingConfig getCustomResource() {
    return JsonUtil.readFromJson("pooling_config/default.json", StackGresPoolingConfig.class);
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
        .statusCode(204);
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
            Matchers.equalTo("[databases]\n"
                + "foodb = dbname=bardb pool_size=10\n"
                + "sgdb = pool_mode=statement max_db_connections=1000\n\n"
                + "[users]\n"
                + "user1 = max_user_connections=30\n"
                + "user2 = pool_mode=session max_user_connections=100\n\n"
                + "[pgbouncer]\n"
                + "default_pool_size = 200\n"
                + "max_client_conn = 200\n"
                + "pool_mode = transaction\n\n"));
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
            Matchers.equalTo("[databases]\n"
                + "foodb = dbname=bardb pool_size=10\n"
                + "sgdb = pool_mode=statement max_db_connections=1000\n\n"
                + "[users]\n"
                + "user1 = max_user_connections=30\n"
                + "user2 = pool_mode=session max_user_connections=100\n\n"
                + "[pgbouncer]\n"
                + "default_pool_size = 200\n"
                + "max_client_conn = 200\n"
                + "pool_mode = transaction\n\n"));
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
        .statusCode(204);

    dto.getSpec().getPgBouncer()
        .setParameters("[databases]\n"
            + "bardb = dbname=foodb pool_size=100\n"
            + "sgdb = pool_mode=statement max_db_connections=500\n\n"
            + "[users]\n"
            + "user1 = max_user_connections=30 pool_mode=transaction\n"
            + "user2 = pool_mode=session max_user_connections=50\n\n"
            + "[pgbouncer]\n"
            + "default_pool_size = 300\n"
            + "max_client_conn = 400\n"
            + "pool_mode = transaction\n\n");

    given()
        .header(AUTHENTICATION_HEADER)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(dto)
        .put("/stackgres/sgpoolconfigs")
        .then()
        .statusCode(204);
  }

}

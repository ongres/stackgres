/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.restassured.http.ContentType;
import io.stackgres.apiweb.dto.Metadata;
import io.stackgres.apiweb.dto.fixture.DtoFixtures;
import io.stackgres.apiweb.dto.stream.StreamDto;
import io.stackgres.apiweb.dto.stream.StreamSourceSgCluster;
import io.stackgres.common.StackGresKubernetesMockServerSetup;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamList;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.StreamScheduler;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@WithKubernetesTestServer(https = true, setup = StackGresKubernetesMockServerSetup.class)
class StreamResourceQuarkusTest implements AuthenticatedResourceTest {

  @KubernetesTestServer
  KubernetesServer mockServer;

  @InjectMock
  StreamScheduler streamScheduler;

  private final StackGresStream stream = getStream();

  @BeforeEach
  void setUp() {
    mockServer.getClient().secrets().inNamespace("test").delete();
    mockServer.getClient().configMaps().inNamespace("test").delete();

    stream.getMetadata().setNamespace("test");
    stream.getMetadata().setName(StringUtils.getRandomResourceName());
    mockServer.getClient().resources(
        StackGresStream.class,
        StackGresStreamList.class)
        .resource(stream)
        .create();
  }

  @AfterEach
  void tearDown() {
    mockServer.getClient().secrets().inNamespace("test").delete();
    mockServer.getClient().configMaps().inNamespace("test").delete();
    mockServer.getClient().resources(
        StackGresStream.class,
        StackGresStreamList.class)
        .inNamespace(stream.getMetadata().getNamespace())
        .withName(stream.getMetadata().getName())
        .delete();
  }

  private StreamDto getStreamDto() {
    return DtoFixtures.stream().loadDefault().get();
  }

  private StackGresStream getStream() {
    return Fixtures.stream().loadSgClusterToCloudEvent().get();
  }

  @Test
  void givenACreationWithInlineStreams_shouldNotFail() {
    StreamDto stream = getStreamDto();
    final Metadata metadata = stream.getMetadata();
    metadata.setNamespace("test");

    when(streamScheduler.create(any(), anyBoolean())).thenReturn(this.stream);

    given()
        .header(AUTHENTICATION_HEADER)
        .body(stream)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post("/stackgres/sgstreams")
        .then().statusCode(200);
  }

  @Test
  void getStream_shouldNotFail() {
    given()
        .header(AUTHENTICATION_HEADER)
        .pathParam("namespace", stream.getMetadata().getNamespace())
        .pathParam("name", stream.getMetadata().getName())
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .get("/stackgres/namespaces/{namespace}/sgstreams/{name}")
        .then()
        .body("metadata.namespace", equalTo("test"),
            "metadata.name", equalTo(stream.getMetadata().getName()))
        .statusCode(200);
  }

  @Test
  void getListStream_shouldNotFail() {
    given()
        .header(AUTHENTICATION_HEADER)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .get("/stackgres/sgstreams")
        .then()
        .body("[0].metadata.namespace", equalTo("test"),
            "[0].metadata.name", equalTo(stream.getMetadata().getName()))
        .statusCode(200);
  }

  @Test
  void givenACreationWithCredentials_shouldNotFail() {
    StreamDto stream = getStreamDto();
    StreamSourceSgCluster sgCluster = getSecretStreamSourceSgCluster();

    stream.getSpec().getSource().setSgCluster(sgCluster);
    final Metadata metadata = stream.getMetadata();
    metadata.setNamespace("test");

    when(streamScheduler.create(any(), anyBoolean())).thenReturn(this.stream);

    given()
        .header(AUTHENTICATION_HEADER)
        .body(stream)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post("/stackgres/sgstreams")
        .then().statusCode(200);

    final SecretKeySelector username = sgCluster.getUsername();
    Secret usernameSecret = mockServer.getClient().secrets().inNamespace("test")
        .withName(username.getName())
        .get();
    assertNotNull(usernameSecret);
    byte[] actualUsernameValue = Base64.getDecoder().decode(usernameSecret.getData().get(username.getKey()));
    assertEquals(sgCluster.getUsernameValue(),
        new String(actualUsernameValue, StandardCharsets.UTF_8));

    final SecretKeySelector password = sgCluster.getPassword();
    Secret passwordSecret = mockServer.getClient().secrets().inNamespace("test")
        .withName(password.getName())
        .get();
    assertNotNull(passwordSecret);
    byte[] actualPasswordValue = Base64.getDecoder().decode(passwordSecret.getData().get(password.getKey()));
    assertEquals(sgCluster.getPasswordValue(),
        new String(actualPasswordValue, StandardCharsets.UTF_8));
  }

  private StreamSourceSgCluster getSecretStreamSourceSgCluster() {
    StreamSourceSgCluster sgCluster = new StreamSourceSgCluster();
    sgCluster.setName("stackgres");
    final SecretKeySelector username = new SecretKeySelector();
    username.setKey("username");
    username.setName("credentials");
    sgCluster.setUsername(username);
    sgCluster.setUsernameValue(StringUtils.getRandomResourceName());
    final SecretKeySelector password = new SecretKeySelector();
    password.setKey("password");
    password.setName("credentials");
    sgCluster.setPassword(password);
    sgCluster.setPasswordValue(StringUtils.getRandomResourceName());
    return sgCluster;
  }

}

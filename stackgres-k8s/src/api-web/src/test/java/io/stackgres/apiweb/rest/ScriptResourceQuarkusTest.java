/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.restassured.http.ContentType;
import io.stackgres.apiweb.dto.Metadata;
import io.stackgres.apiweb.dto.fixture.DtoFixtures;
import io.stackgres.apiweb.dto.script.ScriptDto;
import io.stackgres.apiweb.dto.script.ScriptEntry;
import io.stackgres.apiweb.dto.script.ScriptFrom;
import io.stackgres.common.crd.ConfigMapKeySelector;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptList;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ScriptScheduler;
import io.stackgres.testutil.StackGresKubernetesMockServerSetup;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@WithKubernetesTestServer(https = true, setup = StackGresKubernetesMockServerSetup.class)
class ScriptResourceQuarkusTest implements AuthenticatedResourceTest {

  @KubernetesTestServer
  KubernetesServer mockServer;

  @InjectMock
  ScriptScheduler scriptScheduler;

  private final StackGresScript script = getScript();

  @BeforeEach
  void setUp() {
    mockServer.getClient().secrets().inNamespace("test").delete();
    mockServer.getClient().configMaps().inNamespace("test").delete();

    script.getMetadata().setNamespace("test");
    script.getMetadata().setName(StringUtils.getRandomClusterName());
    mockServer.getClient().resources(
        StackGresScript.class,
        StackGresScriptList.class)
        .resource(script)
        .create();
  }

  @AfterEach
  void tearDown() {
    mockServer.getClient().secrets().inNamespace("test").delete();
    mockServer.getClient().configMaps().inNamespace("test").delete();
    mockServer.getClient().resources(
        StackGresScript.class,
        StackGresScriptList.class)
        .inNamespace(script.getMetadata().getNamespace())
        .withName(script.getMetadata().getName())
        .delete();
  }

  private ScriptDto getScriptInlineScripts() {
    return DtoFixtures.script().loadDefault().get();
  }

  private StackGresScript getScript() {
    return Fixtures.script().loadDefault().get();
  }

  @Test
  void givenACreationWithInlineScripts_shouldNotFail() {
    ScriptDto script = getScriptInlineScripts();
    final Metadata metadata = script.getMetadata();
    metadata.setNamespace("test");

    given()
        .header(AUTHENTICATION_HEADER)
        .body(script)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post("/stackgres/sgscripts")
        .then().statusCode(204);
  }

  @Test
  void getScript_shouldNotFail() {
    given()
        .header(AUTHENTICATION_HEADER)
        .pathParam("namespace", script.getMetadata().getNamespace())
        .pathParam("name", script.getMetadata().getName())
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .get("/stackgres/namespaces/{namespace}/sgscripts/{name}")
        .then()
        .body("metadata.namespace", equalTo("test"),
            "metadata.name", equalTo(script.getMetadata().getName()))
        .statusCode(200);
  }

  @Test
  void getListScript_shouldNotFail() {
    given()
        .header(AUTHENTICATION_HEADER)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .get("/stackgres/sgscripts")
        .then()
        .body("[0].metadata.namespace", equalTo("test"),
            "[0].metadata.name", equalTo(script.getMetadata().getName()))
        .statusCode(200);
  }

  @Test
  void givenACreationWithConfigMapsScripts_shouldNotFail() {
    ScriptDto script = getScriptInlineScripts();
    ScriptEntry entry = getConfigMapScriptEntry();

    script.getSpec().setScripts(Collections.singletonList(entry));
    final Metadata metadata = script.getMetadata();
    metadata.setNamespace("test");

    given()
        .header(AUTHENTICATION_HEADER)
        .body(script)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post("/stackgres/sgscripts")
        .then().statusCode(204);

    ConfigMap configMap = mockServer.getClient().configMaps().inNamespace("test")
        .withName(entry.getScriptFrom().getConfigMapKeyRef().getName())
        .get();
    assertNotNull(configMap);

    String actualConfigScript =
        configMap.getData().get(entry.getScriptFrom().getConfigMapKeyRef().getKey());
    assertEquals(entry.getScriptFrom().getConfigMapScript(), actualConfigScript);
  }

  @Test
  void givenACreationWithSecretScripts_shouldNotFail() {
    ScriptDto script = getScriptInlineScripts();
    ScriptEntry entry = getSecretScriptEntry();

    script.getSpec().setScripts(Collections.singletonList(entry));
    final Metadata metadata = script.getMetadata();
    metadata.setNamespace("test");

    given()
        .header(AUTHENTICATION_HEADER)
        .body(script)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post("/stackgres/sgscripts")
        .then().statusCode(204);

    final ScriptFrom scriptFrom = entry.getScriptFrom();
    final SecretKeySelector secretKeyRef = scriptFrom.getSecretKeyRef();
    Secret secret = mockServer.getClient().secrets().inNamespace("test")
        .withName(secretKeyRef.getName())
        .get();
    assertNotNull(secret);

    byte[] actualScript = Base64.getDecoder().decode(secret.getData().get(secretKeyRef.getKey()));
    assertEquals(scriptFrom.getSecretScript(),
        new String(actualScript, StandardCharsets.UTF_8));
  }

  @Test
  void givenACreationWithSecretAndConfigMapScripts_shouldNotFail() {
    ScriptDto script = getScriptInlineScripts();
    ScriptEntry secretScriptEntry = getSecretScriptEntry();
    ScriptEntry configMapScriptEntry = getConfigMapScriptEntry();

    script.getSpec().setScripts(ImmutableList
        .of(secretScriptEntry, configMapScriptEntry));

    final Metadata metadata = script.getMetadata();
    metadata.setNamespace("test");

    given()
        .header(AUTHENTICATION_HEADER)
        .body(script)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post("/stackgres/sgscripts")
        .then().statusCode(204);

    final ScriptFrom secretScriptFrom = secretScriptEntry.getScriptFrom();
    final SecretKeySelector secretKeyRef = secretScriptFrom.getSecretKeyRef();
    Secret secret = mockServer.getClient().secrets().inNamespace("test")
        .withName(secretKeyRef.getName())
        .get();
    assertNotNull(secret);

    byte[] actualScript = Base64.getDecoder().decode(secret.getData().get(secretKeyRef.getKey()));
    assertEquals(secretScriptFrom.getSecretScript(),
        new String(actualScript, StandardCharsets.UTF_8));

    final ScriptFrom configMapScriptFrom = configMapScriptEntry.getScriptFrom();
    final ConfigMapKeySelector configMapKeyRef = configMapScriptFrom.getConfigMapKeyRef();
    ConfigMap configMap = mockServer.getClient().configMaps().inNamespace("test")
        .withName(configMapKeyRef.getName())
        .get();

    assertNotNull(configMap);

    assertEquals(configMapScriptFrom.getConfigMapScript(),
        configMap.getData().get(configMapKeyRef.getKey()));
  }

  private ScriptEntry getSecretScriptEntry() {
    ScriptEntry entry = new ScriptEntry();
    entry.setName("init");
    final ScriptFrom scriptFrom = new ScriptFrom();
    scriptFrom.setSecretScript("CREATE DATABASE test");
    final SecretKeySelector secretMapKeyRef = new SecretKeySelector();
    scriptFrom.setSecretKeyRef(secretMapKeyRef);
    secretMapKeyRef.setKey("script");
    secretMapKeyRef.setName("initScript");
    entry.setScriptFrom(scriptFrom);
    return entry;
  }

  private ScriptEntry getConfigMapScriptEntry() {
    ScriptEntry entry = new ScriptEntry();
    entry.setName("init");
    final ScriptFrom scriptFrom = new ScriptFrom();
    scriptFrom.setConfigMapScript("CREATE DATABASE test");
    final ConfigMapKeySelector configMapKeyRef = new ConfigMapKeySelector();
    scriptFrom.setConfigMapKeyRef(configMapKeyRef);
    configMapKeyRef.setKey("script");
    configMapKeyRef.setName("initScript");
    entry.setScriptFrom(scriptFrom);
    return entry;
  }

}

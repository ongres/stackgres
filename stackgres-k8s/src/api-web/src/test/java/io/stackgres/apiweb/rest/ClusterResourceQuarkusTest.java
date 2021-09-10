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

import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import io.stackgres.apiweb.dto.Metadata;
import io.stackgres.apiweb.dto.cluster.ClusterDto;
import io.stackgres.apiweb.dto.cluster.ClusterScriptEntry;
import io.stackgres.apiweb.dto.cluster.ClusterScriptFrom;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.ConfigMapKeySelector;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import io.stackgres.common.resource.ClusterScheduler;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ClusterResourceQuarkusTest implements AuthenticatedResourceTest {

  @Inject
  KubernetesClientFactory factory;

  @InjectMock
  ClusterScheduler clusterScheduler;

  private final StackGresCluster cluster = getCluster();

  @BeforeEach
  void setUp() {
    try (KubernetesClient client = factory.create()) {
      client.secrets().inNamespace("test").delete();
      client.configMaps().inNamespace("test").delete();

      cluster.getMetadata().setNamespace("test");
      cluster.getMetadata().setName(StringUtils.getRandomClusterName());
      cluster.getSpec().setConfiguration(new StackGresClusterConfiguration());
      client.customResources(
          StackGresCluster.class,
          StackGresClusterList.class)
          .inNamespace(cluster.getMetadata().getNamespace())
          .withName(cluster.getMetadata().getName())
          .create(cluster);

      Service primary = new ServiceBuilder()
          .withNewMetadata()
          .withName(PatroniUtil.readWriteName(cluster.getMetadata().getName()))
          .withNamespace(cluster.getMetadata().getNamespace())
          .endMetadata()
          .withNewSpec()
          .withType(cluster.getSpec().getPostgresServices().getPrimary().getType() != null
              ? cluster.getSpec().getPostgresServices().getPrimary().getType()
              : null)
          .withClusterIP("10.10.100.8")
          .endSpec()
          .build();
      Service replicas = new ServiceBuilder()
          .withNewMetadata()
          .withName(PatroniUtil.readOnlyName(cluster.getMetadata().getName()))
          .withNamespace(cluster.getMetadata().getNamespace())
          .endMetadata()
          .withNewSpec()
          .withType(cluster.getSpec().getPostgresServices().getReplicas().getType() != null
              ? cluster.getSpec().getPostgresServices().getReplicas().getType()
              : null)
          .withClusterIP("10.10.100.30")
          .endSpec()
          .build();
      client.services().inNamespace(cluster.getMetadata().getNamespace()).create(primary);
      client.services().inNamespace(cluster.getMetadata().getNamespace()).create(replicas);
    }
  }

  @AfterEach
  void tearDown() {
    try (KubernetesClient client = factory.create()) {
      client.secrets().inNamespace("test").delete();
      client.configMaps().inNamespace("test").delete();
      client.customResources(
          StackGresCluster.class,
          StackGresClusterList.class)
          .inNamespace(cluster.getMetadata().getNamespace())
          .withName(cluster.getMetadata().getName())
          .delete();
    }
  }

  private ClusterDto getClusterInlineScripts() {
    return JsonUtil.readFromJson("stackgres_cluster/inline_scripts.json", ClusterDto.class);
  }

  private StackGresCluster getCluster() {
    return JsonUtil.readFromJson("stackgres_cluster/default.json", StackGresCluster.class);
  }

  @Test
  void givenACreationWithInlineScripts_shouldNotFail() {
    ClusterDto cluster = getClusterInlineScripts();
    final Metadata metadata = cluster.getMetadata();
    metadata.setNamespace("test");

    given()
        .header(AUTHENTICATION_HEADER)
        .body(cluster)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post("/stackgres/sgclusters")
        .then().statusCode(204);
  }

  @Test
  void getCluster_shouldNotFail() {
    given()
        .header(AUTHENTICATION_HEADER)
        .pathParam("namespace", cluster.getMetadata().getNamespace())
        .pathParam("name", cluster.getMetadata().getName())
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .get("/stackgres/namespaces/{namespace}/sgclusters/{name}")
        .then()
        .body("metadata.namespace", equalTo("test"),
            "metadata.name", equalTo(cluster.getMetadata().getName()),
            "spec.instances", equalTo(1),
            "spec.postgres.version", equalTo("13.4"),
            "spec.sgInstanceProfile", equalTo("size-xs"),
            "info.superuserSecretName", equalTo(cluster.getMetadata().getName()),
            "info.superuserPasswordKey", equalTo("superuser-password"),
            "info.primaryDns",
            equalTo(PatroniUtil.readWriteName(cluster.getMetadata().getName())
                + ".test.svc.cluster.local"),
            "info.replicasDns",
            equalTo(PatroniUtil.readOnlyName(cluster.getMetadata().getName())
                + ".test.svc.cluster.local"))
        .statusCode(200);
  }

  @Test
  void getListCluster_shouldNotFail() {
    given()
        .header(AUTHENTICATION_HEADER)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .get("/stackgres/sgclusters")
        .then()
        .body("[0].metadata.namespace", equalTo("test"),
            "[0].metadata.name", equalTo(cluster.getMetadata().getName()),
            "[0].spec.instances", equalTo(1),
            "[0].spec.postgres.version", equalTo("13.4"),
            "[0].spec.sgInstanceProfile", equalTo("size-xs"),
            "[0].info.superuserSecretName", equalTo(cluster.getMetadata().getName()),
            "[0].info.superuserPasswordKey", equalTo("superuser-password"),
            "[0].info.primaryDns",
            equalTo(PatroniUtil.readWriteName(cluster.getMetadata().getName())
                + ".test.svc.cluster.local"),
            "[0].info.replicasDns",
            equalTo(PatroniUtil.readOnlyName(cluster.getMetadata().getName())
                + ".test.svc.cluster.local"))
        .statusCode(200);
  }

  @Test
  void givenACreationWithConfigMapsScripts_shouldNotFail() {
    ClusterDto cluster = getClusterInlineScripts();
    ClusterScriptEntry entry = getConfigMapScriptEntry();

    cluster.getSpec().getInitData().setScripts(Collections.singletonList(entry));
    final Metadata metadata = cluster.getMetadata();
    metadata.setNamespace("test");

    given()
        .header(AUTHENTICATION_HEADER)
        .body(cluster)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post("/stackgres/sgclusters")
        .then().statusCode(204);

    try (KubernetesClient client = factory.create()) {
      ConfigMap configMap = client.configMaps().inNamespace("test")
          .withName(entry.getScriptFrom().getConfigMapKeyRef().getName())
          .get();
      assertNotNull(configMap);

      String actualConfigScript =
          configMap.getData().get(entry.getScriptFrom().getConfigMapKeyRef().getKey());
      assertEquals(entry.getScriptFrom().getConfigMapScript(), actualConfigScript);
    }
  }

  @Test
  void givenACreationWithSecretScripts_shouldNotFail() {
    ClusterDto cluster = getClusterInlineScripts();
    ClusterScriptEntry entry = getSecretScriptEntry();

    cluster.getSpec().getInitData().setScripts(Collections.singletonList(entry));
    final Metadata metadata = cluster.getMetadata();
    metadata.setNamespace("test");

    given()
        .header(AUTHENTICATION_HEADER)
        .body(cluster)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post("/stackgres/sgclusters")
        .then().statusCode(204);

    try (KubernetesClient client = factory.create()) {
      final ClusterScriptFrom scriptFrom = entry.getScriptFrom();
      final SecretKeySelector secretKeyRef = scriptFrom.getSecretKeyRef();
      Secret secret = client.secrets().inNamespace("test")
          .withName(secretKeyRef.getName())
          .get();
      assertNotNull(secret);

      byte[] actualScript = Base64.getDecoder().decode(secret.getData().get(secretKeyRef.getKey()));
      assertEquals(scriptFrom.getSecretScript(),
          new String(actualScript, StandardCharsets.UTF_8));
    }
  }

  @Test
  void givenACreationWithSecretAndConfigMapScripts_shouldNotFail() {
    ClusterDto cluster = getClusterInlineScripts();
    ClusterScriptEntry secretScriptEntry = getSecretScriptEntry();
    ClusterScriptEntry configMapScriptEntry = getConfigMapScriptEntry();

    cluster.getSpec().getInitData().setScripts(ImmutableList
        .of(secretScriptEntry, configMapScriptEntry));

    final Metadata metadata = cluster.getMetadata();
    metadata.setNamespace("test");

    given()
        .header(AUTHENTICATION_HEADER)
        .body(cluster)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post("/stackgres/sgclusters")
        .then().statusCode(204);

    try (KubernetesClient client = factory.create()) {
      final ClusterScriptFrom secretScriptFrom = secretScriptEntry.getScriptFrom();
      final SecretKeySelector secretKeyRef = secretScriptFrom.getSecretKeyRef();
      Secret secret = client.secrets().inNamespace("test")
          .withName(secretKeyRef.getName())
          .get();
      assertNotNull(secret);

      byte[] actualScript = Base64.getDecoder().decode(secret.getData().get(secretKeyRef.getKey()));
      assertEquals(secretScriptFrom.getSecretScript(),
          new String(actualScript, StandardCharsets.UTF_8));

      final ClusterScriptFrom configMapScriptFrom = configMapScriptEntry.getScriptFrom();
      final ConfigMapKeySelector configMapKeyRef = configMapScriptFrom.getConfigMapKeyRef();
      ConfigMap configMap = client.configMaps().inNamespace("test")
          .withName(configMapKeyRef.getName())
          .get();

      assertNotNull(configMap);

      assertEquals(configMapScriptFrom.getConfigMapScript(),
          configMap.getData().get(configMapKeyRef.getKey()));
    }
  }

  private ClusterScriptEntry getSecretScriptEntry() {
    ClusterScriptEntry entry = new ClusterScriptEntry();
    entry.setName("init");
    final ClusterScriptFrom scriptFrom = new ClusterScriptFrom();
    scriptFrom.setSecretScript("CREATE DATABASE test");
    final SecretKeySelector secretMapKeyRef = new SecretKeySelector();
    scriptFrom.setSecretKeyRef(secretMapKeyRef);
    secretMapKeyRef.setKey("script");
    secretMapKeyRef.setName("initScript");
    entry.setScriptFrom(scriptFrom);
    return entry;
  }

  private ClusterScriptEntry getConfigMapScriptEntry() {
    ClusterScriptEntry entry = new ClusterScriptEntry();
    entry.setName("init");
    final ClusterScriptFrom scriptFrom = new ClusterScriptFrom();
    scriptFrom.setConfigMapScript("CREATE DATABASE test");
    final ConfigMapKeySelector configMapKeyRef = new ConfigMapKeySelector();
    scriptFrom.setConfigMapKeyRef(configMapKeyRef);
    configMapKeyRef.setKey("script");
    configMapKeyRef.setName("initScript");
    entry.setScriptFrom(scriptFrom);
    return entry;
  }

}

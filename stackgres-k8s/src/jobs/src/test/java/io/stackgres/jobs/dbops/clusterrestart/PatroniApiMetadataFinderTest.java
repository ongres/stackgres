/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.testutil.StringUtils;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WithKubernetesTestServer
@QuarkusTest
class PatroniApiMetadataFinderTest {

  @Inject
  KubernetesClient client;

  @Inject
  PatroniApiMetadataFinder patroniApiFinder;

  String clusterName;
  String namespace;
  Secret secret;
  Service patroniService;

  @BeforeEach
  void setUp() {
    clusterName = StringUtils.getRandomClusterName();
    namespace = StringUtils.getRandomNamespace();
    secret = Fixtures.secret().loadAuthentication().get();
    secret.getMetadata().setName(clusterName);
    secret.getMetadata().setNamespace(namespace);

    patroniService = Fixtures.service().loadPatroniRest().get();
    patroniService.getMetadata().setNamespace(namespace);
    patroniService.getMetadata().setName(clusterName + "-rest");

    createOrUpdate(client.namespaces(),
        new NamespaceBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withName(namespace)
            .build())
        .build());
    createOrUpdate(client.secrets(), secret);
    createOrUpdate(client.services(), patroniService);
  }

  private <T extends HasMetadata, L, R extends Resource<T>> void createOrUpdate(
      NonNamespaceOperation<T, L, R> operation, T resource) {
    var found = operation.resource(resource).get();
    if (found == null) {
      operation.resource(resource).create();
    } else {
      resource.setMetadata(found.getMetadata());
      operation.resource(resource).update();
    }
  }

  @Test
  void givenAValidClusterAndNamespace_shouldBeAbleToReturnThePassword() {

    String password = patroniApiFinder.getPatroniPassword(clusterName, namespace);

    String expectedPassword = getExpectedPassword();

    assertEquals(expectedPassword, password);
  }

  @Test
  void givenAValidClusterAndNamespace_shouldBeAbleToReturnThePatroniPort() {

    int port = patroniApiFinder.getPatroniPort(clusterName, namespace);

    int expectedPort = getExpectedPort();

    assertEquals(expectedPort, port);
  }

  @Test
  void givenAValidClusterAndNamespace_shouldBeAbleToReturnThePatroniApiInfo() {

    PatroniApiMetadata expectedPatroniApiMetadata = ImmutablePatroniApiMetadata.builder()
        .host(clusterName + "-rest." + namespace)
        .port(getExpectedPort())
        .username("superuser")
        .password(getExpectedPassword())
        .build();

    PatroniApiMetadata patroniApiMetadata =
        patroniApiFinder.findPatroniRestApi(clusterName, namespace);
    assertEquals(expectedPatroniApiMetadata, patroniApiMetadata);
  }

  @Test
  void givenAnInvalidCluster_shouldFailToFindPort() {
    var ex = assertThrows(InvalidClusterException.class,
        () -> patroniApiFinder.getPatroniPort("test", namespace));
    assertEquals("Could not find service test-rest in namespace "
        + namespace, ex.getMessage());
  }

  @Test
  void givenAnInvalidClusterState_shouldFailToFindPort() {
    var service = client.services()
        .inNamespace(namespace)
        .withName(patroniService.getMetadata().getName())
        .get();
    service.getSpec().setPorts(List.of(new ServicePortBuilder()
        .withName("nopatroni")
        .withPort(80)
        .build()));
    client.services().inNamespace(namespace)
        .resource(service)
        .update();

    var ex = assertThrows(InvalidClusterException.class,
        () -> patroniApiFinder.getPatroniPort(clusterName, namespace));
    assertEquals("Could not find patroni port in service " + clusterName + "-rest",
        ex.getMessage());
  }

  @Test
  void givenAnInvalidCluster_shouldFailToFindPassword() {
    var ex = assertThrows(InvalidClusterException.class,
        () -> patroniApiFinder.getPatroniPassword("test", namespace));
    assertEquals("Could not find secret test in namespace " + namespace, ex.getMessage());
  }

  @Test
  void givenAnInvalidClusterState_shouldToFindPasword() {
    secret.getData().remove("restapi-password");
    final String name = secret.getMetadata().getName();
    createOrUpdate(client.secrets(), secret);
    var ex = assertThrows(InvalidClusterException.class,
        () -> patroniApiFinder.getPatroniPassword(clusterName, namespace));
    assertEquals("Could not find restapi-password in secret " + name,
        ex.getMessage());
  }

  @NotNull
  private Integer getExpectedPort() {
    return patroniService.getSpec().getPorts().stream()
        .filter(servicePort -> servicePort.getName().equals("patroniport"))
        .findFirst().orElseThrow().getPort();
  }

  @NotNull
  private String getExpectedPassword() {
    return new String(Base64.getDecoder()
        .decode(secret.getData().get("restapi-password")), StandardCharsets.UTF_8);
  }
}

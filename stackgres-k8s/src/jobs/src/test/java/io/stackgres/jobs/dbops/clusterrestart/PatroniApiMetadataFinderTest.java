/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.operatorframework.resource.ResourceUtil;
import io.stackgres.testutil.StringUtils;
import jakarta.inject.Inject;
import org.jooq.lambda.tuple.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WithKubernetesTestServer
@QuarkusTest
class PatroniApiMetadataFinderTest {

  @Inject
  KubernetesClient client;

  @Inject
  PatroniCtlFinder patroniApiFinder;

  String clusterName;
  String namespace;
  StackGresCluster cluster;
  Secret secret;

  @BeforeEach
  void setUp() {
    clusterName = StringUtils.getRandomResourceName();
    namespace = StringUtils.getRandomNamespace();
    cluster = Fixtures.cluster().loadDefault().get();
    cluster.getMetadata().setName(clusterName);
    cluster.getMetadata().setNamespace(namespace);
    secret = new SecretBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(clusterName)
        .endMetadata()
        .withData(ResourceUtil.encodeSecret(Map.of(
            StackGresPasswordKeys.SUPERUSER_USERNAME_KEY, "postgres",
            StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY, "test")))
        .build();

    client.resource(cluster)
        .create();
    client.resource(secret)
        .create();
  }

  @Test
  void givenAValidClusterAndNamespace_shouldBeAbleToReturnThePatroniCtl() {
    var cluster =
        patroniApiFinder.findCluster(clusterName, namespace);
    assertEquals(this.cluster, cluster);
  }

  @Test
  void givenAMissingCluster_shouldThrowAnException() {
    String clusterName = StringUtils.getRandomResourceName();
    var ex = assertThrows(RuntimeException.class,
        () -> patroniApiFinder.findCluster(clusterName, namespace));
    assertEquals("Can not find SGCluster " + clusterName, ex.getMessage());
  }

  @Test
  void givenAMissingClusterNamespace_shouldThrowAnException() {
    var ex = assertThrows(RuntimeException.class,
        () -> patroniApiFinder.findCluster(clusterName, StringUtils.getRandomResourceName()));
    assertEquals("Can not find SGCluster " + clusterName, ex.getMessage());
  }

  @Test
  void givenAValidSecretAndNamespace_shouldBeAbleToReturnThePatroniCtl() {
    var credentials =
        patroniApiFinder.getSuperuserCredentials(clusterName, namespace);
    assertEquals(Tuple.tuple("postgres", "test"), credentials);
  }

  @Test
  void givenAMissingSecret_shouldThrowAnException() {
    String clusterName = StringUtils.getRandomResourceName();
    var ex = assertThrows(RuntimeException.class,
        () -> patroniApiFinder.getSuperuserCredentials(clusterName, namespace));
    assertEquals("Can not find Secret " + clusterName, ex.getMessage());
  }

  @Test
  void givenAMissingSecretClusterNamespace_shouldThrowAnException() {
    var ex = assertThrows(RuntimeException.class,
        () -> patroniApiFinder.getSuperuserCredentials(clusterName, StringUtils.getRandomResourceName()));
    assertEquals("Can not find Secret " + clusterName, ex.getMessage());
  }

  @Test
  void givenASecretWithMissingUsernameKey_shouldThrowAnException() {
    secret = new SecretBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(clusterName)
        .endMetadata()
        .withData(ResourceUtil.encodeSecret(Map.of(
            StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY, "test")))
        .build();

    client.resource(secret)
        .update();
    var ex = assertThrows(RuntimeException.class,
        () -> patroniApiFinder.getSuperuserCredentials(clusterName, namespace));
    assertEquals("Can not find key " + StackGresPasswordKeys.SUPERUSER_USERNAME_KEY
        + " in Secret " + clusterName, ex.getMessage());
  }

  @Test
  void givenASecretWithMissingPasswordKey_shouldThrowAnException() {
    secret = new SecretBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(clusterName)
        .endMetadata()
        .withData(ResourceUtil.encodeSecret(Map.of(
            StackGresPasswordKeys.SUPERUSER_USERNAME_KEY, "postgres")))
        .build();

    client.resource(secret)
        .update();
    var ex = assertThrows(RuntimeException.class,
        () -> patroniApiFinder.getSuperuserCredentials(clusterName, namespace));
    assertEquals("Can not find key " + StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY
        + " in Secret " + clusterName, ex.getMessage());
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterPatroniEnvVarFactoryTest {

  @Mock
  private StackGresClusterContext context;

  private StackGresCluster cluster;

  private final ClusterPatroniEnvVarFactory factory = new ClusterPatroniEnvVarFactory();

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
  }

  @Test
  void patroniNameEnvVar_shouldBeReturned() {

    EnvVar envVar = getEnvVar("PATRONI_NAME");
    assertFieldRef(envVar, "metadata.name");

  }

  @Test
  void patroniNamespaceEnvVar_shouldBeReturned() {

    EnvVar envVar = getEnvVar("PATRONI_KUBERNETES_NAMESPACE");
    assertFieldRef(envVar, "metadata.namespace");

  }

  @Test
  void patroniPodIp_shouldBeReturned() {

    EnvVar envVar = getEnvVar("PATRONI_KUBERNETES_POD_IP");
    assertFieldRef(envVar, "status.podIP");

  }

  @Test
  void patroniSuperUserPassword_shouldBeReturned() {

    EnvVar envVar = getEnvVar("PATRONI_SUPERUSER_PASSWORD");
    assertSecretRef(envVar, "superuser-password");

  }

  @Test
  void patroniReplicationPassword_shouldBeReturned() {

    EnvVar envVar = getEnvVar("PATRONI_REPLICATION_PASSWORD");
    assertSecretRef(envVar, "replication-password");

  }

  @Test
  void patroniAuthenticatorPassword_shouldBeReturned() {

    EnvVar envVar = getEnvVar("PATRONI_authenticator_PASSWORD");
    assertSecretRef(envVar, "authenticator-password");

  }

  @Test
  void patroniAuthenticatorOptions_shouldBeReturned() {

    EnvVar envVar = getEnvVar("PATRONI_authenticator_OPTIONS");
    assertValue(envVar, "superuser");
  }

  private void assertValue(EnvVar envVar, String expectedValue) {
    assertNotNull(envVar.getValue());
    assertNull(envVar.getValueFrom());
    assertEquals(expectedValue, envVar.getValue());
  }

  private void assertSecretRef(EnvVar envVar, String expectedKey) {
    assertNull(envVar.getValue());
    assertNotNull(envVar.getValueFrom());
    assertNull(envVar.getValueFrom().getFieldRef());
    assertNull(envVar.getValueFrom().getConfigMapKeyRef());
    assertNull(envVar.getValueFrom().getResourceFieldRef());
    assertNotNull(envVar.getValueFrom().getSecretKeyRef());
    assertNotNull(envVar.getValueFrom().getSecretKeyRef().getName());
    assertNotNull(envVar.getValueFrom().getSecretKeyRef().getKey());
    assertEquals(cluster.getMetadata().getName(),
        envVar.getValueFrom().getSecretKeyRef().getName());
    assertEquals(expectedKey, envVar.getValueFrom().getSecretKeyRef().getKey());
  }

  private EnvVar getEnvVar(String patroniName) {
    when(context.getSource()).thenReturn(cluster);
    Stream<EnvVar> envVars = factory.createResource(context).stream();

    return envVars.filter(envVar -> envVar.getName().equals(patroniName))
        .findFirst().orElseThrow();
  }

  private void assertFieldRef(EnvVar envVar, String expectedPath) {
    assertNull(envVar.getValue());
    assertNotNull(envVar.getValueFrom());
    assertNotNull(envVar.getValueFrom().getFieldRef());
    assertNull(envVar.getValueFrom().getSecretKeyRef());
    assertNull(envVar.getValueFrom().getConfigMapKeyRef());
    assertNull(envVar.getValueFrom().getResourceFieldRef());
    assertNotNull(envVar.getValueFrom().getFieldRef().getFieldPath());
    assertEquals(expectedPath, envVar.getValueFrom().getFieldRef().getFieldPath());
  }
}

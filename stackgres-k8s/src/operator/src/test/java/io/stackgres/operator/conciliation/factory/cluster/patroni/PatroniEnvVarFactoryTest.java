/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFrom;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecBuilder;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatroniEnvVarFactoryTest {

  @Mock
  private StackGresClusterContext context;

  private StackGresCluster cluster;

  private StackGresPostgresConfig postgresConfig;

  private final PatroniEnvironmentVariables factory =
      new PatroniEnvironmentVariables();

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    postgresConfig = Fixtures.postgresConfig().loadDefault().get();
  }

  @Test
  void patroniNameEnvVar_shouldBeReturned() {
    EnvVar envVar = getEnvVar("PATRONI_NAME");
    assertFieldRef(envVar, "metadata.name");
  }

  @Test
  void patroniPodIp_shouldBeReturned() {
    EnvVar envVar = getEnvVar("POD_IP");
    assertFieldRef(envVar, "status.podIP");
  }

  @Test
  void patroniAuthenticatorPassword_shouldNotBeReturned() {
    assertNotPresent(
        "PATRONI_" + StackGresPasswordKeys.AUTHENTICATOR_USERNAME + "_PASSWORD");
  }

  @Test
  void patroniAuthenticatorOptions_shouldNotBeReturned() {
    assertNotPresent(
        "PATRONI_" + StackGresPasswordKeys.AUTHENTICATOR_USERNAME + "_OPTIONS");
  }

  @Test
  void patroniRestApiConnectAddressEnvVar_shouldBeReturned() {
    EnvVar envVar = getEnvVar("PATRONI_RESTAPI_CONNECT_ADDRESS");
    assertValue(envVar, "${POD_IP}:" + EnvoyUtil.PATRONI_PORT);
  }

  @Test
  void patroniRestApiUsernameEnvVar_shouldNotBeReturned() {
    assertNotPresent("PATRONI_RESTAPI_USERNAME");
  }

  @Test
  void patroniRestApiPasswordEnvVar_shouldNotBeReturned() {
    assertNotPresent("PATRONI_RESTAPI_PASSWORD");
  }

  @Test
  void patroniRecoveryFromBackupEnvVar_shouldNotBeReturned() {
    cluster.setSpec(new StackGresClusterSpecBuilder(cluster.getSpec())
        .withInitialData(null)
        .build());
    assertNotPresent("RECOVERY_FROM_BACKUP");
  }

  @Test
  void patroniReplicateFromBackupEnvVar_shouldNotBeReturned() {
    assertNotPresent("REPLICATE_FROM_BACKUP");
  }

  @Test
  void patroniRecoveryFromBackupEnvVar_shouldBeReturned() {
    EnvVar envVar = getEnvVar("RECOVERY_FROM_BACKUP");
    assertValue(envVar, "true");
  }

  @Test
  void patroniReplicateFromBackupEnvVar_shouldBeReturned() {
    cluster.setSpec(new StackGresClusterSpecBuilder(cluster.getSpec())
        .withReplicateFrom(new StackGresClusterReplicateFrom())
        .build());
    EnvVar envVar = getEnvVar("REPLICATE_FROM_BACKUP");
    assertValue(envVar, "true");
  }

  @Test
  void patroniRecoveryTargetEnvVar_shouldNotBeReturned() {
    assertNotPresent("RECOVERY_TARGET");
  }

  @Test
  void patroniRecoveryTargetTimelineEnvVar_shouldNotBeReturned() {
    assertNotPresent("RECOVERY_TARGET_TIMELINE");
  }

  @Test
  void patroniRecoveryTargetInclusiveEnvVar_shouldNotBeReturned() {
    assertNotPresent("RECOVERY_TARGET_INCLUSIVE");
  }

  @Test
  void patroniRecoveryTargetNameEnvVar_shouldNotBeReturned() {
    assertNotPresent("RECOVERY_TARGET_NAME");
  }

  @Test
  void patroniRecoveryTargetXidEnvVar_shouldNotBeReturned() {
    assertNotPresent("RECOVERY_TARGET_XID");
  }

  @Test
  void patroniRecoveryTargetLsnEnvVar_shouldNotBeReturned() {
    assertNotPresent("RECOVERY_TARGET_LSN");
  }

  @Test
  void patroniRecoveryTargetTimeEnvVar_shouldNotBeReturned() {
    assertNotPresent("RECOVERY_TARGET_TIME");
  }

  @Test
  void patroniRecoveryTargetEnvVar_shouldBeReturned() {
    cluster.setSpec(new StackGresClusterSpecBuilder(cluster.getSpec())
        .withNewInitialData()
        .withNewRestore()
        .withNewFromBackup()
        .withTarget("test")
        .endFromBackup()
        .endRestore()
        .endInitialData()
        .build());
    EnvVar envVar = getEnvVar("RECOVERY_TARGET");
    assertValue(envVar, "test");
  }

  @Test
  void patroniRecoveryTargetTimelineEnvVar_shouldBeReturned() {
    cluster.setSpec(new StackGresClusterSpecBuilder(cluster.getSpec())
        .withNewInitialData()
        .withNewRestore()
        .withNewFromBackup()
        .withTargetTimeline("test")
        .endFromBackup()
        .endRestore()
        .endInitialData()
        .build());
    EnvVar envVar = getEnvVar("RECOVERY_TARGET_TIMELINE");
    assertValue(envVar, "test");
  }

  @Test
  void patroniRecoveryTargetInclusiveEnvVar_shouldBeReturned() {
    cluster.setSpec(new StackGresClusterSpecBuilder(cluster.getSpec())
        .withNewInitialData()
        .withNewRestore()
        .withNewFromBackup()
        .withTargetInclusive(true)
        .endFromBackup()
        .endRestore()
        .endInitialData()
        .build());
    EnvVar envVar = getEnvVar("RECOVERY_TARGET_INCLUSIVE");
    assertValue(envVar, "on");
  }

  @Test
  void patroniRecoveryTargetNameEnvVar_shouldBeReturned() {
    cluster.setSpec(new StackGresClusterSpecBuilder(cluster.getSpec())
        .withNewInitialData()
        .withNewRestore()
        .withNewFromBackup()
        .withTargetName("test")
        .endFromBackup()
        .endRestore()
        .endInitialData()
        .build());
    EnvVar envVar = getEnvVar("RECOVERY_TARGET_NAME");
    assertValue(envVar, "test");
  }

  @Test
  void patroniRecoveryTargetXidEnvVar_shouldBeReturned() {
    cluster.setSpec(new StackGresClusterSpecBuilder(cluster.getSpec())
        .withNewInitialData()
        .withNewRestore()
        .withNewFromBackup()
        .withTargetXid("test")
        .endFromBackup()
        .endRestore()
        .endInitialData()
        .build());
    EnvVar envVar = getEnvVar("RECOVERY_TARGET_XID");
    assertValue(envVar, "test");
  }

  @Test
  void patroniRecoveryTargetLsnEnvVar_shouldBeReturned() {
    cluster.setSpec(new StackGresClusterSpecBuilder(cluster.getSpec())
        .withNewInitialData()
        .withNewRestore()
        .withNewFromBackup()
        .withTargetLsn("test")
        .endFromBackup()
        .endRestore()
        .endInitialData()
        .build());
    EnvVar envVar = getEnvVar("RECOVERY_TARGET_LSN");
    assertValue(envVar, "test");
  }

  @Test
  void patroniRecoveryTargetTimeEnvVar_shouldBeReturned() {
    String testTime = "2022-08-25T12:52:00Z";
    String expectedEnvVarValue = "2022-08-25 12:52:00";
    cluster.setSpec(new StackGresClusterSpecBuilder(cluster.getSpec())
        .withNewInitialData()
        .withNewRestore()
        .withNewFromBackup()
        .withNewPointInTimeRecovery()
        .withRestoreToTimestamp(testTime)
        .endPointInTimeRecovery()
        .endFromBackup()
        .endRestore()
        .endInitialData()
        .build());
    EnvVar envVar = getEnvVar("RECOVERY_TARGET_TIME");
    assertValue(envVar, expectedEnvVarValue);
  }

  private void assertValue(EnvVar envVar, String expectedValue) {
    assertNotNull(envVar.getValue());
    assertNull(envVar.getValueFrom());
    assertEquals(expectedValue, envVar.getValue());
  }

  private EnvVar getEnvVar(String envVarName) {
    when(context.getSource()).thenReturn(cluster);
    when(context.getPostgresConfig()).thenReturn(postgresConfig);
    Stream<EnvVar> envVars = factory.getEnvVars(context).stream();

    var envVarFound = envVars.filter(envVar -> envVar.getName().equals(envVarName))
        .findFirst();
    assertTrue(envVarFound.isPresent());
    return envVarFound.orElseThrow();
  }

  private void assertNotPresent(String envVarName) {
    when(context.getSource()).thenReturn(cluster);
    when(context.getPostgresConfig()).thenReturn(postgresConfig);
    Stream<EnvVar> envVars = factory.getEnvVars(context).stream();

    var envVarFound = envVars.filter(envVar -> envVar.getName().equals(envVarName))
        .findFirst();
    assertTrue(envVarFound.isEmpty());
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

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostgresEnvironmentVariablesTest {

  @Mock
  private StackGresClusterContext context;

  private StackGresCluster cluster;

  private PostgresEnvironmentVariables factory;

  @BeforeEach
  void setUp() {
    factory = new PostgresEnvironmentVariables();
    cluster = Fixtures.cluster().loadDefault().get();
    when(context.getCluster()).thenReturn(cluster);
  }

  @Test
  void getEnvVars_shouldReturnTwoEnvVars() {
    List<EnvVar> envVars = factory.getEnvVars(context);

    assertEquals(2, envVars.size());
  }

  @Test
  void getEnvVars_shouldContainPgUserAndPgDatabase() {
    List<EnvVar> envVars = factory.getEnvVars(context);
    List<String> envVarNames = envVars.stream()
        .map(EnvVar::getName)
        .toList();

    assertTrue(envVarNames.contains("PGUSER"));
    assertTrue(envVarNames.contains("PGDATABASE"));
  }

  @Test
  void getEnvVars_pgUserShouldReferenceCorrectSecretKey() {
    List<EnvVar> envVars = factory.getEnvVars(context);

    Optional<EnvVar> pgUser = envVars.stream()
        .filter(e -> e.getName().equals("PGUSER"))
        .findFirst();
    assertTrue(pgUser.isPresent());

    EnvVar envVar = pgUser.get();
    assertNull(envVar.getValue());
    assertNotNull(envVar.getValueFrom());
    assertNotNull(envVar.getValueFrom().getSecretKeyRef());
    assertEquals(PatroniSecret.name(cluster),
        envVar.getValueFrom().getSecretKeyRef().getName());
    assertEquals(StackGresPasswordKeys.SUPERUSER_USERNAME_KEY,
        envVar.getValueFrom().getSecretKeyRef().getKey());
    assertFalse(envVar.getValueFrom().getSecretKeyRef().getOptional());
  }

  @Test
  void getEnvVars_pgDatabaseShouldBePostgres() {
    List<EnvVar> envVars = factory.getEnvVars(context);

    Optional<EnvVar> pgDatabase = envVars.stream()
        .filter(e -> e.getName().equals("PGDATABASE"))
        .findFirst();
    assertTrue(pgDatabase.isPresent());

    EnvVar envVar = pgDatabase.get();
    assertEquals("postgres", envVar.getValue());
    assertNull(envVar.getValueFrom());
  }
}

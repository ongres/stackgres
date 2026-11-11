/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static io.stackgres.common.StringUtil.generateRandom;
import static io.stackgres.common.patroni.StackGresPasswordKeys.AUTHENTICATOR_OPTIONS_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.AUTHENTICATOR_USERNAME;
import static io.stackgres.common.patroni.StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.AUTHENTICATOR_USERNAME_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.BABELFISH_CREATE_USER_SQL_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.BABELFISH_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.MONITOR_PASSWORD_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.MONITOR_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.MONITOR_USERNAME_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.MONITOR_USERNAME_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.PGBOUNCER_ADMIN_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.PGBOUNCER_STATS_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.REPLICATION_PASSWORD_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.REPLICATION_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.REPLICATION_PASSWORD_YAML_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.REPLICATION_PREVIOUS_USERNAME_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.REPLICATION_USERNAME_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.REPLICATION_USERNAME_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.REPLICATION_USERNAME_YAML_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.RESTAPI_PASSWORD_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.RESTAPI_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.RESTAPI_PASSWORD_YAML_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.RESTAPI_USERNAME_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.RESTAPI_USERNAME_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.RESTAPI_USERNAME_YAML_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.ROLES_UPDATE_SQL_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_PASSWORD_YAML_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_PREVIOUS_USERNAME_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_USERNAME_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_USERNAME_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_USERNAME_YAML_ENV;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.YamlScalarWriter;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFrom;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromExternal;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromInstance;
import io.stackgres.common.crd.sgcluster.StackGresPostgresFlavor;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatroniSecretTest {

  private final Secret existentSecret = new SecretBuilder()
      .addToData(ResourceUtil.encodeSecret(ImmutableMap.of(
          SUPERUSER_USERNAME_KEY, generateRandom(),
          SUPERUSER_PASSWORD_KEY, generateRandom(),
          REPLICATION_USERNAME_KEY, generateRandom(),
          REPLICATION_PASSWORD_KEY, generateRandom(),
          AUTHENTICATOR_USERNAME_KEY, generateRandom(),
          AUTHENTICATOR_PASSWORD_KEY, generateRandom(),
          PGBOUNCER_ADMIN_PASSWORD_KEY, generateRandom(),
          PGBOUNCER_STATS_PASSWORD_KEY, generateRandom(),
          RESTAPI_PASSWORD_KEY, generateRandom(),
          BABELFISH_CREATE_USER_SQL_KEY, generateRandom())))
      .build();

  private final Map<String, String> decodedExistentSecretData =
      ResourceUtil.decodeSecret(existentSecret.getData());

  @Mock
  private LabelFactoryForCluster labelFactory;

  @Mock
  private StackGresClusterContext context;

  private final PatroniSecret patroniSecret = new PatroniSecret();

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    patroniSecret.setFactoryFactory(labelFactory);
    patroniSecret.setYamlScalarWriter(new YamlScalarWriter(new YamlMapperProvider()));
    when(labelFactory.genericLabels(any(StackGresCluster.class))).thenReturn(ImmutableMap.of());

    when(context.getSource()).thenReturn(cluster);
    lenient().when(context.getGeneratedSuperuserPassword()).thenReturn(generateRandom());
    lenient().when(context.getGeneratedReplicationPassword()).thenReturn(generateRandom());
    lenient().when(context.getGeneratedAuthenticatorPassword()).thenReturn(generateRandom());
    lenient().when(context.getGeneratedMonitorPassword()).thenReturn(generateRandom());
    lenient().when(context.getGeneratedPgBouncerAdminPassword()).thenReturn(generateRandom());
    lenient().when(context.getGeneratedPgBouncerStatsPassword()).thenReturn(generateRandom());
    lenient().when(context.getGeneratedPatroniRestApiPassword()).thenReturn(generateRandom());
    lenient().when(context.getGeneratedBabelfishPassword()).thenReturn(generateRandom());
  }

  @Test
  void generateResources_shouldGenerateRandomPasswords() {
    Secret secret = patroniSecret.buildSource(context);

    final Map<String, String> data = ResourceUtil.decodeSecret(secret.getData());
    assertTrue(data.containsKey(SUPERUSER_USERNAME_ENV));
    assertTrue(data.containsKey(SUPERUSER_USERNAME_KEY));
    assertTrue(data.containsKey(SUPERUSER_PASSWORD_ENV));
    assertTrue(data.containsKey(SUPERUSER_PASSWORD_KEY));
    assertTrue(data.containsKey(REPLICATION_USERNAME_ENV));
    assertTrue(data.containsKey(REPLICATION_USERNAME_KEY));
    assertTrue(data.containsKey(REPLICATION_PASSWORD_ENV));
    assertTrue(data.containsKey(REPLICATION_PASSWORD_KEY));
    assertTrue(data.containsKey(AUTHENTICATOR_USERNAME_ENV));
    assertTrue(data.containsKey(AUTHENTICATOR_USERNAME_KEY));
    assertTrue(data.containsKey(AUTHENTICATOR_PASSWORD_ENV
        .replace(AUTHENTICATOR_USERNAME, data.get(AUTHENTICATOR_USERNAME_ENV))));
    assertTrue(data.containsKey(AUTHENTICATOR_OPTIONS_ENV
        .replace(AUTHENTICATOR_USERNAME, data.get(AUTHENTICATOR_USERNAME_ENV))));
    assertTrue(data.containsKey(AUTHENTICATOR_PASSWORD_KEY));
    assertTrue(data.containsKey(MONITOR_USERNAME_ENV));
    assertTrue(data.containsKey(MONITOR_USERNAME_KEY));
    assertTrue(data.containsKey(MONITOR_PASSWORD_ENV));
    assertTrue(data.containsKey(MONITOR_PASSWORD_KEY));
    assertTrue(data.containsKey(PGBOUNCER_ADMIN_PASSWORD_KEY));
    assertTrue(data.containsKey(PGBOUNCER_STATS_PASSWORD_KEY));
    assertFalse(data.containsKey(BABELFISH_PASSWORD_KEY));
    assertFalse(data.containsKey(BABELFISH_CREATE_USER_SQL_KEY));
    assertTrue(data.containsKey(RESTAPI_USERNAME_ENV));
    assertTrue(data.containsKey(RESTAPI_USERNAME_KEY));
    assertTrue(data.containsKey(RESTAPI_PASSWORD_ENV));
    assertTrue(data.containsKey(RESTAPI_PASSWORD_KEY));

    final Map<String, String> existentData = decodedExistentSecretData;
    assertNotEquals(existentData.get(SUPERUSER_USERNAME_KEY), data.get(SUPERUSER_USERNAME_ENV));
    assertNotEquals(existentData.get(SUPERUSER_USERNAME_KEY), data.get(SUPERUSER_USERNAME_KEY));
    assertNotEquals(existentData.get(SUPERUSER_PASSWORD_KEY), data.get(SUPERUSER_PASSWORD_ENV));
    assertNotEquals(existentData.get(SUPERUSER_PASSWORD_KEY), data.get(SUPERUSER_PASSWORD_KEY));
    assertNotEquals(existentData.get(REPLICATION_USERNAME_KEY), data.get(REPLICATION_USERNAME_ENV));
    assertNotEquals(existentData.get(REPLICATION_USERNAME_KEY), data.get(REPLICATION_USERNAME_KEY));
    assertNotEquals(existentData.get(REPLICATION_PASSWORD_KEY), data.get(REPLICATION_PASSWORD_ENV));
    assertNotEquals(existentData.get(REPLICATION_PASSWORD_KEY), data.get(REPLICATION_PASSWORD_KEY));
    assertNotEquals(
        existentData.get(AUTHENTICATOR_USERNAME_KEY), data.get(AUTHENTICATOR_USERNAME_ENV));
    assertNotEquals(
        existentData.get(AUTHENTICATOR_USERNAME_KEY), data.get(AUTHENTICATOR_USERNAME_KEY));
    assertNotEquals(
        existentData.get(AUTHENTICATOR_PASSWORD_KEY), data.get(AUTHENTICATOR_PASSWORD_ENV
            .replace(AUTHENTICATOR_USERNAME, data.get(AUTHENTICATOR_USERNAME_ENV))));
    assertNotEquals(
        existentData.get(AUTHENTICATOR_PASSWORD_KEY), data.get(AUTHENTICATOR_PASSWORD_KEY));
    assertNotEquals(existentData.get(PGBOUNCER_ADMIN_PASSWORD_KEY),
        data.get(PGBOUNCER_ADMIN_PASSWORD_KEY));
    assertNotEquals(existentData.get(PGBOUNCER_STATS_PASSWORD_KEY),
        data.get(PGBOUNCER_STATS_PASSWORD_KEY));
    assertNotEquals(existentData.get(RESTAPI_PASSWORD_KEY), data.get(RESTAPI_PASSWORD_ENV));
    assertNotEquals(existentData.get(RESTAPI_PASSWORD_KEY), data.get(RESTAPI_PASSWORD_KEY));
  }

  @Test
  void generateResourcesWithExistentSecret_shouldReusePasswords() {
    when(context.getDatabaseSecret()).thenReturn(Optional.of(existentSecret));
    Secret secret = patroniSecret.buildSource(context);

    final Map<String, String> data = ResourceUtil.decodeSecret(secret.getData());
    assertTrue(data.containsKey(SUPERUSER_USERNAME_ENV));
    assertTrue(data.containsKey(SUPERUSER_USERNAME_KEY));
    assertTrue(data.containsKey(SUPERUSER_PASSWORD_ENV));
    assertTrue(data.containsKey(SUPERUSER_PASSWORD_KEY));
    assertTrue(data.containsKey(REPLICATION_USERNAME_ENV));
    assertTrue(data.containsKey(REPLICATION_USERNAME_KEY));
    assertTrue(data.containsKey(REPLICATION_PASSWORD_ENV));
    assertTrue(data.containsKey(REPLICATION_PASSWORD_KEY));
    assertTrue(data.containsKey(AUTHENTICATOR_USERNAME_ENV));
    assertTrue(data.containsKey(AUTHENTICATOR_USERNAME_KEY));
    assertTrue(data.containsKey(AUTHENTICATOR_PASSWORD_ENV
        .replace(AUTHENTICATOR_USERNAME, data.get(AUTHENTICATOR_USERNAME_ENV))));
    assertTrue(data.containsKey(AUTHENTICATOR_OPTIONS_ENV
        .replace(AUTHENTICATOR_USERNAME, data.get(AUTHENTICATOR_USERNAME_ENV))));
    assertTrue(data.containsKey(AUTHENTICATOR_PASSWORD_KEY));
    assertTrue(data.containsKey(MONITOR_USERNAME_ENV));
    assertTrue(data.containsKey(MONITOR_USERNAME_KEY));
    assertTrue(data.containsKey(MONITOR_PASSWORD_ENV));
    assertTrue(data.containsKey(MONITOR_PASSWORD_KEY));
    assertTrue(data.containsKey(PGBOUNCER_ADMIN_PASSWORD_KEY));
    assertTrue(data.containsKey(PGBOUNCER_STATS_PASSWORD_KEY));
    assertFalse(data.containsKey(BABELFISH_PASSWORD_KEY));
    assertFalse(data.containsKey(BABELFISH_CREATE_USER_SQL_KEY));
    assertTrue(data.containsKey(RESTAPI_USERNAME_ENV));
    assertTrue(data.containsKey(RESTAPI_USERNAME_KEY));
    assertTrue(data.containsKey(RESTAPI_PASSWORD_ENV));
    assertTrue(data.containsKey(RESTAPI_PASSWORD_KEY));

    final Map<String, String> existentData = decodedExistentSecretData;
    assertEquals(existentData.get(SUPERUSER_USERNAME_KEY), data.get(SUPERUSER_USERNAME_ENV));
    assertEquals(existentData.get(SUPERUSER_USERNAME_KEY), data.get(SUPERUSER_USERNAME_KEY));
    assertEquals(existentData.get(SUPERUSER_PASSWORD_KEY), data.get(SUPERUSER_PASSWORD_ENV));
    assertEquals(existentData.get(SUPERUSER_PASSWORD_KEY), data.get(SUPERUSER_PASSWORD_KEY));
    assertEquals(existentData.get(REPLICATION_USERNAME_KEY), data.get(REPLICATION_USERNAME_ENV));
    assertEquals(existentData.get(REPLICATION_USERNAME_KEY), data.get(REPLICATION_USERNAME_KEY));
    assertEquals(existentData.get(REPLICATION_PASSWORD_KEY), data.get(REPLICATION_PASSWORD_ENV));
    assertEquals(existentData.get(REPLICATION_PASSWORD_KEY), data.get(REPLICATION_PASSWORD_KEY));
    assertEquals(
        existentData.get(AUTHENTICATOR_USERNAME_KEY), data.get(AUTHENTICATOR_USERNAME_ENV));
    assertEquals(
        existentData.get(AUTHENTICATOR_USERNAME_KEY), data.get(AUTHENTICATOR_USERNAME_KEY));
    assertEquals(
        existentData.get(AUTHENTICATOR_PASSWORD_KEY), data.get(AUTHENTICATOR_PASSWORD_ENV
            .replace(AUTHENTICATOR_USERNAME, data.get(AUTHENTICATOR_USERNAME_ENV))));
    assertEquals(
        existentData.get(AUTHENTICATOR_PASSWORD_KEY), data.get(AUTHENTICATOR_PASSWORD_KEY));
    assertEquals(existentData.get(PGBOUNCER_ADMIN_PASSWORD_KEY),
        data.get(PGBOUNCER_ADMIN_PASSWORD_KEY));
    assertEquals(existentData.get(PGBOUNCER_STATS_PASSWORD_KEY),
        data.get(PGBOUNCER_STATS_PASSWORD_KEY));
    assertEquals(existentData.get(RESTAPI_PASSWORD_KEY), data.get(RESTAPI_PASSWORD_ENV));
    assertEquals(existentData.get(RESTAPI_PASSWORD_KEY), data.get(RESTAPI_PASSWORD_KEY));
  }

  @Test
  void generateResourcesWithCredentialsSecret_shouldReusePasswords() {
    cluster.getSpec().setReplicateFrom(new StackGresClusterReplicateFrom());
    cluster.getSpec().getReplicateFrom().setInstance(new StackGresClusterReplicateFromInstance());
    cluster.getSpec().getReplicateFrom().getInstance()
        .setExternal(new StackGresClusterReplicateFromExternal());
    cluster.getSpec().getReplicateFrom().getInstance().getExternal()
        .setHost("test");
    cluster.getSpec().getReplicateFrom().getInstance().getExternal()
        .setPort(5433);
    when(context.getSuperuserUsername())
        .thenReturn(Optional.of(decodedExistentSecretData.get(SUPERUSER_USERNAME_KEY)));
    when(context.getSuperuserPassword())
        .thenReturn(Optional.of(decodedExistentSecretData.get(SUPERUSER_PASSWORD_KEY)));
    when(context.getReplicationUsername())
        .thenReturn(Optional.of(decodedExistentSecretData.get(REPLICATION_USERNAME_KEY)));
    when(context.getReplicationPassword())
        .thenReturn(Optional.of(decodedExistentSecretData.get(REPLICATION_PASSWORD_KEY)));
    when(context.getAuthenticatorUsername())
        .thenReturn(Optional.of(decodedExistentSecretData.get(AUTHENTICATOR_USERNAME_KEY)));
    when(context.getAuthenticatorPassword())
        .thenReturn(Optional.of(decodedExistentSecretData.get(AUTHENTICATOR_PASSWORD_KEY)));
    when(context.getPatroniRestApiPassword())
        .thenReturn(Optional.of(decodedExistentSecretData.get(RESTAPI_PASSWORD_KEY)));
    Secret secret = patroniSecret.buildSource(context);

    final Map<String, String> data = ResourceUtil.decodeSecret(secret.getData());
    assertTrue(data.containsKey(SUPERUSER_USERNAME_ENV));
    assertTrue(data.containsKey(SUPERUSER_USERNAME_KEY));
    assertTrue(data.containsKey(SUPERUSER_PASSWORD_ENV));
    assertTrue(data.containsKey(SUPERUSER_PASSWORD_KEY));
    assertTrue(data.containsKey(REPLICATION_USERNAME_ENV));
    assertTrue(data.containsKey(REPLICATION_USERNAME_KEY));
    assertTrue(data.containsKey(REPLICATION_PASSWORD_ENV));
    assertTrue(data.containsKey(REPLICATION_PASSWORD_KEY));
    assertTrue(data.containsKey(AUTHENTICATOR_USERNAME_ENV));
    assertTrue(data.containsKey(AUTHENTICATOR_USERNAME_KEY));
    assertTrue(data.containsKey(AUTHENTICATOR_PASSWORD_ENV
        .replace(AUTHENTICATOR_USERNAME, data.get(AUTHENTICATOR_USERNAME_ENV))));
    assertTrue(data.containsKey(AUTHENTICATOR_OPTIONS_ENV
        .replace(AUTHENTICATOR_USERNAME, data.get(AUTHENTICATOR_USERNAME_ENV))));
    assertTrue(data.containsKey(AUTHENTICATOR_PASSWORD_KEY));
    assertTrue(data.containsKey(MONITOR_USERNAME_ENV));
    assertTrue(data.containsKey(MONITOR_USERNAME_KEY));
    assertTrue(data.containsKey(MONITOR_PASSWORD_ENV));
    assertTrue(data.containsKey(MONITOR_PASSWORD_KEY));
    assertTrue(data.containsKey(PGBOUNCER_ADMIN_PASSWORD_KEY));
    assertTrue(data.containsKey(PGBOUNCER_STATS_PASSWORD_KEY));
    assertFalse(data.containsKey(BABELFISH_PASSWORD_KEY));
    assertFalse(data.containsKey(BABELFISH_CREATE_USER_SQL_KEY));
    assertTrue(data.containsKey(RESTAPI_USERNAME_ENV));
    assertTrue(data.containsKey(RESTAPI_USERNAME_KEY));
    assertTrue(data.containsKey(RESTAPI_PASSWORD_ENV));
    assertTrue(data.containsKey(RESTAPI_PASSWORD_KEY));

    final Map<String, String> existentData = decodedExistentSecretData;
    assertEquals(existentData.get(SUPERUSER_USERNAME_KEY), data.get(SUPERUSER_USERNAME_ENV));
    assertEquals(existentData.get(SUPERUSER_USERNAME_KEY), data.get(SUPERUSER_USERNAME_KEY));
    assertEquals(existentData.get(SUPERUSER_PASSWORD_KEY), data.get(SUPERUSER_PASSWORD_ENV));
    assertEquals(existentData.get(SUPERUSER_PASSWORD_KEY), data.get(SUPERUSER_PASSWORD_KEY));
    assertEquals(existentData.get(REPLICATION_USERNAME_KEY), data.get(REPLICATION_USERNAME_ENV));
    assertEquals(existentData.get(REPLICATION_USERNAME_KEY), data.get(REPLICATION_USERNAME_KEY));
    assertEquals(existentData.get(REPLICATION_PASSWORD_KEY), data.get(REPLICATION_PASSWORD_ENV));
    assertEquals(existentData.get(REPLICATION_PASSWORD_KEY), data.get(REPLICATION_PASSWORD_KEY));
    assertEquals(
        existentData.get(AUTHENTICATOR_USERNAME_KEY), data.get(AUTHENTICATOR_USERNAME_ENV));
    assertEquals(
        existentData.get(AUTHENTICATOR_USERNAME_KEY), data.get(AUTHENTICATOR_USERNAME_KEY));
    assertEquals(
        existentData.get(AUTHENTICATOR_PASSWORD_KEY), data.get(AUTHENTICATOR_PASSWORD_ENV
            .replace(AUTHENTICATOR_USERNAME, data.get(AUTHENTICATOR_USERNAME_ENV))));
    assertEquals(
        existentData.get(AUTHENTICATOR_PASSWORD_KEY), data.get(AUTHENTICATOR_PASSWORD_KEY));
    assertNotEquals(existentData.get(PGBOUNCER_ADMIN_PASSWORD_KEY),
        data.get(PGBOUNCER_ADMIN_PASSWORD_KEY));
    assertNotEquals(existentData.get(PGBOUNCER_STATS_PASSWORD_KEY),
        data.get(PGBOUNCER_STATS_PASSWORD_KEY));
    assertEquals(existentData.get(RESTAPI_PASSWORD_KEY), data.get(RESTAPI_PASSWORD_ENV));
    assertEquals(existentData.get(RESTAPI_PASSWORD_KEY), data.get(RESTAPI_PASSWORD_KEY));
  }

  @Test
  void generateResourcesForBabelfishFlavor_shouldGenerateRandomPasswords() {
    cluster.getSpec().getPostgres().setFlavor(StackGresPostgresFlavor.BABELFISH.toString());
    Secret secret = patroniSecret.buildSource(context);

    final Map<String, String> data = ResourceUtil.decodeSecret(secret.getData());
    assertTrue(data.containsKey(SUPERUSER_USERNAME_ENV));
    assertTrue(data.containsKey(SUPERUSER_USERNAME_KEY));
    assertTrue(data.containsKey(SUPERUSER_PASSWORD_ENV));
    assertTrue(data.containsKey(SUPERUSER_PASSWORD_KEY));
    assertTrue(data.containsKey(REPLICATION_USERNAME_ENV));
    assertTrue(data.containsKey(REPLICATION_USERNAME_KEY));
    assertTrue(data.containsKey(REPLICATION_PASSWORD_ENV));
    assertTrue(data.containsKey(REPLICATION_PASSWORD_KEY));
    assertTrue(data.containsKey(AUTHENTICATOR_USERNAME_ENV));
    assertTrue(data.containsKey(AUTHENTICATOR_USERNAME_KEY));
    assertTrue(data.containsKey(AUTHENTICATOR_PASSWORD_ENV
        .replace(AUTHENTICATOR_USERNAME, data.get(AUTHENTICATOR_USERNAME_ENV))));
    assertTrue(data.containsKey(AUTHENTICATOR_OPTIONS_ENV
        .replace(AUTHENTICATOR_USERNAME, data.get(AUTHENTICATOR_USERNAME_ENV))));
    assertTrue(data.containsKey(AUTHENTICATOR_PASSWORD_KEY));
    assertTrue(data.containsKey(MONITOR_USERNAME_ENV));
    assertTrue(data.containsKey(MONITOR_USERNAME_KEY));
    assertTrue(data.containsKey(MONITOR_PASSWORD_ENV));
    assertTrue(data.containsKey(MONITOR_PASSWORD_KEY));
    assertTrue(data.containsKey(PGBOUNCER_ADMIN_PASSWORD_KEY));
    assertTrue(data.containsKey(PGBOUNCER_STATS_PASSWORD_KEY));
    assertTrue(data.containsKey(BABELFISH_PASSWORD_KEY));
    assertTrue(data.containsKey(BABELFISH_CREATE_USER_SQL_KEY));
    assertTrue(data.containsKey(RESTAPI_USERNAME_KEY));
    assertTrue(data.containsKey(RESTAPI_USERNAME_ENV));
    assertTrue(data.containsKey(RESTAPI_PASSWORD_ENV));
    assertTrue(data.containsKey(RESTAPI_PASSWORD_KEY));

    final Map<String, String> existentData = decodedExistentSecretData;
    assertNotEquals(existentData.get(SUPERUSER_USERNAME_KEY), data.get(SUPERUSER_USERNAME_ENV));
    assertNotEquals(existentData.get(SUPERUSER_USERNAME_KEY), data.get(SUPERUSER_USERNAME_KEY));
    assertNotEquals(existentData.get(SUPERUSER_PASSWORD_KEY), data.get(SUPERUSER_PASSWORD_ENV));
    assertNotEquals(existentData.get(SUPERUSER_PASSWORD_KEY), data.get(SUPERUSER_PASSWORD_KEY));
    assertNotEquals(existentData.get(REPLICATION_USERNAME_KEY), data.get(REPLICATION_USERNAME_ENV));
    assertNotEquals(existentData.get(REPLICATION_USERNAME_KEY), data.get(REPLICATION_USERNAME_KEY));
    assertNotEquals(existentData.get(REPLICATION_PASSWORD_KEY), data.get(REPLICATION_PASSWORD_ENV));
    assertNotEquals(existentData.get(REPLICATION_PASSWORD_KEY), data.get(REPLICATION_PASSWORD_KEY));
    assertNotEquals(
        existentData.get(AUTHENTICATOR_USERNAME_KEY), data.get(AUTHENTICATOR_USERNAME_ENV));
    assertNotEquals(
        existentData.get(AUTHENTICATOR_USERNAME_KEY), data.get(AUTHENTICATOR_USERNAME_KEY));
    assertNotEquals(
        existentData.get(AUTHENTICATOR_PASSWORD_KEY), data.get(AUTHENTICATOR_PASSWORD_ENV
            .replace(AUTHENTICATOR_USERNAME, data.get(AUTHENTICATOR_USERNAME_ENV))));
    assertNotEquals(
        existentData.get(AUTHENTICATOR_PASSWORD_KEY), data.get(AUTHENTICATOR_PASSWORD_KEY));
    assertNotEquals(existentData.get(PGBOUNCER_ADMIN_PASSWORD_KEY),
        data.get(PGBOUNCER_ADMIN_PASSWORD_KEY));
    assertNotEquals(existentData.get(PGBOUNCER_STATS_PASSWORD_KEY),
        data.get(PGBOUNCER_STATS_PASSWORD_KEY));
    assertNotEquals(existentData.get(BABELFISH_PASSWORD_KEY),
        data.get(BABELFISH_PASSWORD_KEY));
    assertNotEquals(existentData.get(RESTAPI_PASSWORD_KEY), data.get(RESTAPI_PASSWORD_ENV));
    assertNotEquals(existentData.get(RESTAPI_PASSWORD_KEY), data.get(RESTAPI_PASSWORD_KEY));
  }

  @Test
  void generateResourcesWithUsernamesChanged_shouldCreateTheUsersAndKeepThePreviousUsernames() {
    when(context.getDatabaseSecret()).thenReturn(Optional.of(new SecretBuilder()
        .addToData(ResourceUtil.encodeSecret(Map.of(
            SUPERUSER_USERNAME_KEY, "postgres",
            SUPERUSER_PASSWORD_KEY, "s",
            REPLICATION_USERNAME_KEY, "replicator",
            REPLICATION_PASSWORD_KEY, "r")))
        .build()));
    when(context.getSuperuserUsername()).thenReturn(Optional.of("postgres2"));
    when(context.getReplicationUsername()).thenReturn(Optional.of("replicator2"));
    Secret secret = patroniSecret.buildSource(context);

    final Map<String, String> data = ResourceUtil.decodeSecret(secret.getData());
    assertEquals("postgres", data.get(SUPERUSER_PREVIOUS_USERNAME_KEY));
    assertEquals("replicator", data.get(REPLICATION_PREVIOUS_USERNAME_KEY));
    final String rolesUpdateSql = data.get(ROLES_UPDATE_SQL_KEY);
    assertTrue(rolesUpdateSql.contains(
        "IF NOT EXISTS (SELECT * FROM pg_roles WHERE rolname = 'postgres2') THEN\n"
        + "    CREATE ROLE \"postgres2\" WITH SUPERUSER INHERIT CREATEROLE CREATEDB LOGIN"
        + " REPLICATION BYPASSRLS PASSWORD 's';\n"
        + "  ELSE\n"
        + "    ALTER ROLE \"postgres2\" WITH PASSWORD 's';\n"), rolesUpdateSql);
    assertTrue(rolesUpdateSql.contains(
        "IF NOT EXISTS (SELECT * FROM pg_roles WHERE rolname = 'replicator2') THEN\n"
        + "    CREATE ROLE \"replicator2\" WITH NOSUPERUSER INHERIT NOCREATEROLE NOCREATEDB LOGIN"
        + " REPLICATION NOBYPASSRLS PASSWORD 'r';\n"
        + "  ELSE\n"
        + "    ALTER ROLE \"replicator2\" WITH PASSWORD 'r';\n"), rolesUpdateSql);
    assertEquals(Optional.of("replicator"), PatroniSecret.getPreviousReplicatorUsername(context));
  }

  @Test
  void generateResourcesWithUsernamesUnchanged_shouldKeepThePreviousUsernames() {
    when(context.getDatabaseSecret()).thenReturn(Optional.of(new SecretBuilder()
        .addToData(ResourceUtil.encodeSecret(Map.of(
            SUPERUSER_USERNAME_KEY, "postgres2",
            SUPERUSER_PASSWORD_KEY, "s",
            SUPERUSER_PREVIOUS_USERNAME_KEY, "postgres",
            REPLICATION_USERNAME_KEY, "replicator2",
            REPLICATION_PASSWORD_KEY, "r",
            REPLICATION_PREVIOUS_USERNAME_KEY, "replicator")))
        .build()));
    Secret secret = patroniSecret.buildSource(context);

    final Map<String, String> data = ResourceUtil.decodeSecret(secret.getData());
    assertEquals("postgres2", data.get(SUPERUSER_USERNAME_KEY));
    assertEquals("postgres", data.get(SUPERUSER_PREVIOUS_USERNAME_KEY));
    assertEquals("replicator2", data.get(REPLICATION_USERNAME_KEY));
    assertEquals("replicator", data.get(REPLICATION_PREVIOUS_USERNAME_KEY));
  }

  @Test
  void generateResourcesWithoutPreviousSecret_shouldNotHavePreviousUsernames() {
    Secret secret = patroniSecret.buildSource(context);

    final Map<String, String> data = ResourceUtil.decodeSecret(secret.getData());
    assertFalse(data.containsKey(SUPERUSER_PREVIOUS_USERNAME_KEY));
    assertFalse(data.containsKey(REPLICATION_PREVIOUS_USERNAME_KEY));
    assertEquals(Optional.empty(), PatroniSecret.getPreviousReplicatorUsername(context));
  }

  @Test
  void generateResources_shouldHaveTheCredentialsAsSingleLineYamlScalars() throws Exception {
    final String password = "a'b\"c\\d:{e}, f#g\nh\u0007";
    when(context.getSuperuserPassword()).thenReturn(Optional.of(password));
    when(context.getReplicationPassword()).thenReturn(Optional.of(password));
    when(context.getPatroniRestApiPassword()).thenReturn(Optional.of(password));
    Secret secret = patroniSecret.buildSource(context);

    final Map<String, String> data = ResourceUtil.decodeSecret(secret.getData());
    final YAMLMapper yamlMapper = new YamlMapperProvider().get();
    for (var keys : Map.of(
        SUPERUSER_USERNAME_YAML_ENV, SUPERUSER_USERNAME_ENV,
        SUPERUSER_PASSWORD_YAML_ENV, SUPERUSER_PASSWORD_ENV,
        REPLICATION_USERNAME_YAML_ENV, REPLICATION_USERNAME_ENV,
        REPLICATION_PASSWORD_YAML_ENV, REPLICATION_PASSWORD_ENV,
        RESTAPI_USERNAME_YAML_ENV, RESTAPI_USERNAME_ENV,
        RESTAPI_PASSWORD_YAML_ENV, RESTAPI_PASSWORD_ENV).entrySet()) {
      final String yamlValue = data.get(keys.getKey());
      assertFalse(yamlValue.contains("\n"), keys.getKey() + ": " + yamlValue);
      assertEquals(data.get(keys.getValue()),
          yamlMapper.readTree("{ value: " + yamlValue + " }").get("value").asText(),
          keys.getKey() + ": " + yamlValue);
    }
    assertEquals(password, data.get(SUPERUSER_PASSWORD_ENV));
  }

}

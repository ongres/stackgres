/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static io.stackgres.common.patroni.StackGresPasswordKeys.AUTHENTICATOR_OPTIONS_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.AUTHENTICATOR_USERNAME;
import static io.stackgres.common.patroni.StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.BABELFISH_CREATE_USER_SQL_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.BABELFISH_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.PGBOUNCER_ADMIN_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.PGBOUNCER_STATS_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.REPLICATION_PASSWORD_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.REPLICATION_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.REPLICATION_USERNAME_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.RESTAPI_PASSWORD_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.RESTAPI_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.RESTAPI_USERNAME_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_USERNAME_ENV;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.StringUtil;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFrom;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromExternal;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromInstance;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromUserSecretKeyRef;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromUsers;
import io.stackgres.common.crd.sgcluster.StackGresPostgresFlavor;
import io.stackgres.common.fixture.Fixtures;
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
          SUPERUSER_USERNAME_ENV, StringUtil.generateRandom(),
          SUPERUSER_PASSWORD_KEY, StringUtil.generateRandom(),
          REPLICATION_USERNAME_ENV, StringUtil.generateRandom(),
          REPLICATION_PASSWORD_KEY, StringUtil.generateRandom(),
          AUTHENTICATOR_USERNAME_ENV, StringUtil.generateRandom(),
          AUTHENTICATOR_PASSWORD_KEY, StringUtil.generateRandom(),
          PGBOUNCER_ADMIN_PASSWORD_KEY, StringUtil.generateRandom(),
          PGBOUNCER_STATS_PASSWORD_KEY, StringUtil.generateRandom(),
          RESTAPI_PASSWORD_KEY, StringUtil.generateRandom(),
          BABELFISH_CREATE_USER_SQL_KEY, StringUtil.generateRandom())))
      .build();

  private final Map<String, String> decodedExistentSecretData =
      ResourceUtil.decodeSecret(existentSecret.getData());

  @Mock
  private LabelFactoryForCluster<StackGresCluster> labelFactory;

  @Mock
  private StackGresClusterContext generatorContext;

  private final PatroniSecret patroniSecret = new PatroniSecret();

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    patroniSecret.setFactoryFactory(labelFactory);
    when(labelFactory.genericLabels(any(StackGresCluster.class))).thenReturn(ImmutableMap.of());

    when(generatorContext.getSource()).thenReturn(cluster);
  }

  @Test
  void generateResources_shouldGenerateRandomPasswords() {
    Secret secret = patroniSecret.buildSource(generatorContext);

    final Map<String, String> data = ResourceUtil.decodeSecret(secret.getData());
    assertTrue(data.containsKey(SUPERUSER_USERNAME_ENV));
    assertTrue(data.containsKey(SUPERUSER_PASSWORD_ENV));
    assertTrue(data.containsKey(SUPERUSER_PASSWORD_KEY));
    assertTrue(data.containsKey(REPLICATION_USERNAME_ENV));
    assertTrue(data.containsKey(REPLICATION_PASSWORD_ENV));
    assertTrue(data.containsKey(REPLICATION_PASSWORD_KEY));
    assertTrue(data.containsKey(AUTHENTICATOR_USERNAME_ENV));
    assertTrue(data.containsKey(AUTHENTICATOR_PASSWORD_ENV
        .replace(AUTHENTICATOR_USERNAME, data.get(AUTHENTICATOR_USERNAME_ENV))));
    assertTrue(data.containsKey(AUTHENTICATOR_OPTIONS_ENV
        .replace(AUTHENTICATOR_USERNAME, data.get(AUTHENTICATOR_USERNAME_ENV))));
    assertTrue(data.containsKey(AUTHENTICATOR_PASSWORD_KEY));
    assertTrue(data.containsKey(PGBOUNCER_ADMIN_PASSWORD_KEY));
    assertTrue(data.containsKey(PGBOUNCER_STATS_PASSWORD_KEY));
    assertFalse(data.containsKey(BABELFISH_PASSWORD_KEY));
    assertFalse(data.containsKey(BABELFISH_CREATE_USER_SQL_KEY));
    assertTrue(data.containsKey(RESTAPI_USERNAME_ENV));
    assertTrue(data.containsKey(RESTAPI_PASSWORD_ENV));
    assertTrue(data.containsKey(RESTAPI_PASSWORD_KEY));

    final Map<String, String> existentData = decodedExistentSecretData;
    assertNotEquals(existentData.get(SUPERUSER_USERNAME_ENV), data.get(SUPERUSER_USERNAME_ENV));
    assertNotEquals(existentData.get(SUPERUSER_PASSWORD_KEY), data.get(SUPERUSER_PASSWORD_ENV));
    assertNotEquals(existentData.get(SUPERUSER_PASSWORD_KEY), data.get(SUPERUSER_PASSWORD_KEY));
    assertNotEquals(existentData.get(REPLICATION_USERNAME_ENV), data.get(REPLICATION_USERNAME_ENV));
    assertNotEquals(existentData.get(REPLICATION_PASSWORD_KEY), data.get(REPLICATION_PASSWORD_ENV));
    assertNotEquals(existentData.get(REPLICATION_PASSWORD_KEY), data.get(REPLICATION_PASSWORD_KEY));
    assertNotEquals(
        existentData.get(AUTHENTICATOR_USERNAME_ENV), data.get(AUTHENTICATOR_USERNAME_ENV));
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
    when(generatorContext.getDatabaseSecret()).thenReturn(Optional.of(existentSecret));
    Secret secret = patroniSecret.buildSource(generatorContext);

    final Map<String, String> data = ResourceUtil.decodeSecret(secret.getData());
    assertTrue(data.containsKey(SUPERUSER_USERNAME_ENV));
    assertTrue(data.containsKey(SUPERUSER_PASSWORD_ENV));
    assertTrue(data.containsKey(SUPERUSER_PASSWORD_KEY));
    assertTrue(data.containsKey(REPLICATION_USERNAME_ENV));
    assertTrue(data.containsKey(REPLICATION_PASSWORD_ENV));
    assertTrue(data.containsKey(REPLICATION_PASSWORD_KEY));
    assertTrue(data.containsKey(AUTHENTICATOR_USERNAME_ENV));
    assertTrue(data.containsKey(AUTHENTICATOR_PASSWORD_ENV
        .replace(AUTHENTICATOR_USERNAME, data.get(AUTHENTICATOR_USERNAME_ENV))));
    assertTrue(data.containsKey(AUTHENTICATOR_OPTIONS_ENV
        .replace(AUTHENTICATOR_USERNAME, data.get(AUTHENTICATOR_USERNAME_ENV))));
    assertTrue(data.containsKey(AUTHENTICATOR_PASSWORD_KEY));
    assertTrue(data.containsKey(PGBOUNCER_ADMIN_PASSWORD_KEY));
    assertTrue(data.containsKey(PGBOUNCER_STATS_PASSWORD_KEY));
    assertFalse(data.containsKey(BABELFISH_PASSWORD_KEY));
    assertFalse(data.containsKey(BABELFISH_CREATE_USER_SQL_KEY));
    assertTrue(data.containsKey(RESTAPI_USERNAME_ENV));
    assertTrue(data.containsKey(RESTAPI_PASSWORD_ENV));
    assertTrue(data.containsKey(RESTAPI_PASSWORD_KEY));

    final Map<String, String> existentData = decodedExistentSecretData;
    assertEquals(existentData.get(SUPERUSER_USERNAME_ENV), data.get(SUPERUSER_USERNAME_ENV));
    assertEquals(existentData.get(SUPERUSER_PASSWORD_KEY), data.get(SUPERUSER_PASSWORD_ENV));
    assertEquals(existentData.get(SUPERUSER_PASSWORD_KEY), data.get(SUPERUSER_PASSWORD_KEY));
    assertEquals(existentData.get(REPLICATION_USERNAME_ENV), data.get(REPLICATION_USERNAME_ENV));
    assertEquals(existentData.get(REPLICATION_PASSWORD_KEY), data.get(REPLICATION_PASSWORD_ENV));
    assertEquals(existentData.get(REPLICATION_PASSWORD_KEY), data.get(REPLICATION_PASSWORD_KEY));
    assertEquals(
        existentData.get(AUTHENTICATOR_USERNAME_ENV), data.get(AUTHENTICATOR_USERNAME_ENV));
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
  void generateResourcesWithReplicateFromSecret_shouldReusePasswords() {
    cluster.getSpec().setReplicateFrom(new StackGresClusterReplicateFrom());
    cluster.getSpec().getReplicateFrom().setInstance(new StackGresClusterReplicateFromInstance());
    cluster.getSpec().getReplicateFrom().getInstance()
        .setExternal(new StackGresClusterReplicateFromExternal());
    cluster.getSpec().getReplicateFrom().getInstance().getExternal()
        .setHost("test");
    cluster.getSpec().getReplicateFrom().getInstance().getExternal()
        .setPort(5433);
    cluster.getSpec().getReplicateFrom()
        .setUsers(new StackGresClusterReplicateFromUsers());
    cluster.getSpec().getReplicateFrom().getUsers()
        .setSuperuser(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getSuperuser().setUsername(new SecretKeySelector(SUPERUSER_USERNAME_ENV, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getSuperuser().setPassword(new SecretKeySelector(SUPERUSER_PASSWORD_KEY, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .setReplication(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getReplication().setUsername(new SecretKeySelector(REPLICATION_USERNAME_ENV, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getReplication().setPassword(new SecretKeySelector(REPLICATION_PASSWORD_KEY, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .setAuthenticator(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getAuthenticator().setUsername(new SecretKeySelector(AUTHENTICATOR_USERNAME_ENV, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getAuthenticator().setPassword(new SecretKeySelector(AUTHENTICATOR_PASSWORD_KEY, "test"));
    when(generatorContext.getExternalSuperuserUsernameSecret())
        .thenReturn(Optional.of(existentSecret));
    when(generatorContext.getExternalSuperuserPasswordSecret())
        .thenReturn(Optional.of(existentSecret));
    when(generatorContext.getExternalReplicationUsernameSecret())
        .thenReturn(Optional.of(existentSecret));
    when(generatorContext.getExternalReplicationPasswordSecret())
        .thenReturn(Optional.of(existentSecret));
    when(generatorContext.getExternalAuthenticatorUsernameSecret())
        .thenReturn(Optional.of(existentSecret));
    when(generatorContext.getExternalAuthenticatorPasswordSecret())
        .thenReturn(Optional.of(existentSecret));
    Secret secret = patroniSecret.buildSource(generatorContext);

    final Map<String, String> data = ResourceUtil.decodeSecret(secret.getData());
    assertTrue(data.containsKey(SUPERUSER_USERNAME_ENV));
    assertTrue(data.containsKey(SUPERUSER_PASSWORD_ENV));
    assertTrue(data.containsKey(SUPERUSER_PASSWORD_KEY));
    assertTrue(data.containsKey(REPLICATION_USERNAME_ENV));
    assertTrue(data.containsKey(REPLICATION_PASSWORD_ENV));
    assertTrue(data.containsKey(REPLICATION_PASSWORD_KEY));
    assertTrue(data.containsKey(AUTHENTICATOR_USERNAME_ENV));
    assertTrue(data.containsKey(AUTHENTICATOR_PASSWORD_ENV
        .replace(AUTHENTICATOR_USERNAME, data.get(AUTHENTICATOR_USERNAME_ENV))));
    assertTrue(data.containsKey(AUTHENTICATOR_OPTIONS_ENV
        .replace(AUTHENTICATOR_USERNAME, data.get(AUTHENTICATOR_USERNAME_ENV))));
    assertTrue(data.containsKey(AUTHENTICATOR_PASSWORD_KEY));
    assertTrue(data.containsKey(PGBOUNCER_ADMIN_PASSWORD_KEY));
    assertTrue(data.containsKey(PGBOUNCER_STATS_PASSWORD_KEY));
    assertFalse(data.containsKey(BABELFISH_PASSWORD_KEY));
    assertFalse(data.containsKey(BABELFISH_CREATE_USER_SQL_KEY));
    assertTrue(data.containsKey(RESTAPI_USERNAME_ENV));
    assertTrue(data.containsKey(RESTAPI_PASSWORD_ENV));
    assertTrue(data.containsKey(RESTAPI_PASSWORD_KEY));

    final Map<String, String> existentData = decodedExistentSecretData;
    assertEquals(existentData.get(SUPERUSER_USERNAME_ENV), data.get(SUPERUSER_USERNAME_ENV));
    assertEquals(existentData.get(SUPERUSER_PASSWORD_KEY), data.get(SUPERUSER_PASSWORD_ENV));
    assertEquals(existentData.get(SUPERUSER_PASSWORD_KEY), data.get(SUPERUSER_PASSWORD_KEY));
    assertEquals(existentData.get(REPLICATION_USERNAME_ENV), data.get(REPLICATION_USERNAME_ENV));
    assertEquals(existentData.get(REPLICATION_PASSWORD_KEY), data.get(REPLICATION_PASSWORD_ENV));
    assertEquals(existentData.get(REPLICATION_PASSWORD_KEY), data.get(REPLICATION_PASSWORD_KEY));
    assertEquals(
        existentData.get(AUTHENTICATOR_USERNAME_ENV), data.get(AUTHENTICATOR_USERNAME_ENV));
    assertEquals(
        existentData.get(AUTHENTICATOR_PASSWORD_KEY), data.get(AUTHENTICATOR_PASSWORD_ENV
            .replace(AUTHENTICATOR_USERNAME, data.get(AUTHENTICATOR_USERNAME_ENV))));
    assertEquals(
        existentData.get(AUTHENTICATOR_PASSWORD_KEY), data.get(AUTHENTICATOR_PASSWORD_KEY));
    assertNotEquals(existentData.get(PGBOUNCER_ADMIN_PASSWORD_KEY),
        data.get(PGBOUNCER_ADMIN_PASSWORD_KEY));
    assertNotEquals(existentData.get(PGBOUNCER_STATS_PASSWORD_KEY),
        data.get(PGBOUNCER_STATS_PASSWORD_KEY));
    assertNotEquals(existentData.get(RESTAPI_PASSWORD_KEY), data.get(RESTAPI_PASSWORD_ENV));
    assertNotEquals(existentData.get(RESTAPI_PASSWORD_KEY), data.get(RESTAPI_PASSWORD_KEY));
  }

  @Test
  void generateResourcesWithReplicateFromSecretAndNoSuperuserUsername_shouldThrowAnException() {
    cluster.getSpec().setReplicateFrom(new StackGresClusterReplicateFrom());
    cluster.getSpec().getReplicateFrom().setInstance(new StackGresClusterReplicateFromInstance());
    cluster.getSpec().getReplicateFrom().getInstance()
        .setExternal(new StackGresClusterReplicateFromExternal());
    cluster.getSpec().getReplicateFrom().getInstance().getExternal()
        .setHost("test");
    cluster.getSpec().getReplicateFrom().getInstance().getExternal()
        .setPort(5433);
    cluster.getSpec().getReplicateFrom()
        .setUsers(new StackGresClusterReplicateFromUsers());
    cluster.getSpec().getReplicateFrom().getUsers()
        .setSuperuser(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getSuperuser().setUsername(new SecretKeySelector(SUPERUSER_USERNAME_ENV, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getSuperuser().setPassword(new SecretKeySelector(SUPERUSER_PASSWORD_KEY, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .setReplication(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getReplication().setUsername(new SecretKeySelector(REPLICATION_USERNAME_ENV, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getReplication().setPassword(new SecretKeySelector(REPLICATION_PASSWORD_KEY, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .setAuthenticator(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getAuthenticator().setUsername(new SecretKeySelector(AUTHENTICATOR_USERNAME_ENV, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getAuthenticator().setPassword(new SecretKeySelector(AUTHENTICATOR_PASSWORD_KEY, "test"));

    assertThrows(IllegalArgumentException.class,
        () -> patroniSecret.buildSource(generatorContext));
  }

  @Test
  void generateResourcesWithReplicateFromSecretAndNoSuperuserPassword_shouldThrowAnException() {
    cluster.getSpec().setReplicateFrom(new StackGresClusterReplicateFrom());
    cluster.getSpec().getReplicateFrom().setInstance(new StackGresClusterReplicateFromInstance());
    cluster.getSpec().getReplicateFrom().getInstance()
        .setExternal(new StackGresClusterReplicateFromExternal());
    cluster.getSpec().getReplicateFrom().getInstance().getExternal()
        .setHost("test");
    cluster.getSpec().getReplicateFrom().getInstance().getExternal()
        .setPort(5433);
    cluster.getSpec().getReplicateFrom()
        .setUsers(new StackGresClusterReplicateFromUsers());
    cluster.getSpec().getReplicateFrom().getUsers()
        .setSuperuser(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getSuperuser().setUsername(new SecretKeySelector(SUPERUSER_USERNAME_ENV, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getSuperuser().setPassword(new SecretKeySelector(SUPERUSER_PASSWORD_KEY, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .setReplication(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getReplication().setUsername(new SecretKeySelector(REPLICATION_USERNAME_ENV, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getReplication().setPassword(new SecretKeySelector(REPLICATION_PASSWORD_KEY, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .setAuthenticator(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getAuthenticator().setUsername(new SecretKeySelector(AUTHENTICATOR_USERNAME_ENV, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getAuthenticator().setPassword(new SecretKeySelector(AUTHENTICATOR_PASSWORD_KEY, "test"));
    when(generatorContext.getExternalSuperuserUsernameSecret())
        .thenReturn(Optional.of(existentSecret));

    assertThrows(IllegalArgumentException.class,
        () -> patroniSecret.buildSource(generatorContext));
  }

  @Test
  void generateResourcesWithReplicateFromSecretAndNoReplicationUsername_shouldThrowAnException() {
    cluster.getSpec().setReplicateFrom(new StackGresClusterReplicateFrom());
    cluster.getSpec().getReplicateFrom().setInstance(new StackGresClusterReplicateFromInstance());
    cluster.getSpec().getReplicateFrom().getInstance()
        .setExternal(new StackGresClusterReplicateFromExternal());
    cluster.getSpec().getReplicateFrom().getInstance().getExternal()
        .setHost("test");
    cluster.getSpec().getReplicateFrom().getInstance().getExternal()
        .setPort(5433);
    cluster.getSpec().getReplicateFrom()
        .setUsers(new StackGresClusterReplicateFromUsers());
    cluster.getSpec().getReplicateFrom().getUsers()
        .setSuperuser(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getSuperuser().setUsername(new SecretKeySelector(SUPERUSER_USERNAME_ENV, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getSuperuser().setPassword(new SecretKeySelector(SUPERUSER_PASSWORD_KEY, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .setReplication(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getReplication().setUsername(new SecretKeySelector(REPLICATION_USERNAME_ENV, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getReplication().setPassword(new SecretKeySelector(REPLICATION_PASSWORD_KEY, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .setAuthenticator(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getAuthenticator().setUsername(new SecretKeySelector(AUTHENTICATOR_USERNAME_ENV, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getAuthenticator().setPassword(new SecretKeySelector(AUTHENTICATOR_PASSWORD_KEY, "test"));
    when(generatorContext.getExternalSuperuserUsernameSecret())
        .thenReturn(Optional.of(existentSecret));
    when(generatorContext.getExternalSuperuserPasswordSecret())
        .thenReturn(Optional.of(existentSecret));

    assertThrows(IllegalArgumentException.class,
        () -> patroniSecret.buildSource(generatorContext));
  }

  @Test
  void generateResourcesWithReplicateFromSecretAndNoReplicationPassword_shouldThrowAnException() {
    cluster.getSpec().setReplicateFrom(new StackGresClusterReplicateFrom());
    cluster.getSpec().getReplicateFrom().setInstance(new StackGresClusterReplicateFromInstance());
    cluster.getSpec().getReplicateFrom().getInstance()
        .setExternal(new StackGresClusterReplicateFromExternal());
    cluster.getSpec().getReplicateFrom().getInstance().getExternal()
        .setHost("test");
    cluster.getSpec().getReplicateFrom().getInstance().getExternal()
        .setPort(5433);
    cluster.getSpec().getReplicateFrom()
        .setUsers(new StackGresClusterReplicateFromUsers());
    cluster.getSpec().getReplicateFrom().getUsers()
        .setSuperuser(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getSuperuser().setUsername(new SecretKeySelector(SUPERUSER_USERNAME_ENV, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getSuperuser().setPassword(new SecretKeySelector(SUPERUSER_PASSWORD_KEY, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .setReplication(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getReplication().setUsername(new SecretKeySelector(REPLICATION_USERNAME_ENV, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getReplication().setPassword(new SecretKeySelector(REPLICATION_PASSWORD_KEY, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .setAuthenticator(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getAuthenticator().setUsername(new SecretKeySelector(AUTHENTICATOR_USERNAME_ENV, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getAuthenticator().setPassword(new SecretKeySelector(AUTHENTICATOR_PASSWORD_KEY, "test"));
    when(generatorContext.getExternalSuperuserUsernameSecret())
        .thenReturn(Optional.of(existentSecret));
    when(generatorContext.getExternalSuperuserPasswordSecret())
        .thenReturn(Optional.of(existentSecret));
    when(generatorContext.getExternalReplicationUsernameSecret())
        .thenReturn(Optional.of(existentSecret));

    assertThrows(IllegalArgumentException.class,
        () -> patroniSecret.buildSource(generatorContext));
  }

  @Test
  void generateResourcesWithReplicateFromSecretAndNoAuthenticatorUsername_shouldThrowAnException() {
    cluster.getSpec().setReplicateFrom(new StackGresClusterReplicateFrom());
    cluster.getSpec().getReplicateFrom().setInstance(new StackGresClusterReplicateFromInstance());
    cluster.getSpec().getReplicateFrom().getInstance()
        .setExternal(new StackGresClusterReplicateFromExternal());
    cluster.getSpec().getReplicateFrom().getInstance().getExternal()
        .setHost("test");
    cluster.getSpec().getReplicateFrom().getInstance().getExternal()
        .setPort(5433);
    cluster.getSpec().getReplicateFrom()
        .setUsers(new StackGresClusterReplicateFromUsers());
    cluster.getSpec().getReplicateFrom().getUsers()
        .setSuperuser(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getSuperuser().setUsername(new SecretKeySelector(SUPERUSER_USERNAME_ENV, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getSuperuser().setPassword(new SecretKeySelector(SUPERUSER_PASSWORD_KEY, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .setReplication(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getReplication().setUsername(new SecretKeySelector(REPLICATION_USERNAME_ENV, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getReplication().setPassword(new SecretKeySelector(REPLICATION_PASSWORD_KEY, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .setAuthenticator(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getAuthenticator().setUsername(new SecretKeySelector(AUTHENTICATOR_USERNAME_ENV, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getAuthenticator().setPassword(new SecretKeySelector(AUTHENTICATOR_PASSWORD_KEY, "test"));
    when(generatorContext.getExternalSuperuserUsernameSecret())
        .thenReturn(Optional.of(existentSecret));
    when(generatorContext.getExternalSuperuserPasswordSecret())
        .thenReturn(Optional.of(existentSecret));
    when(generatorContext.getExternalReplicationUsernameSecret())
        .thenReturn(Optional.of(existentSecret));
    when(generatorContext.getExternalReplicationPasswordSecret())
        .thenReturn(Optional.of(existentSecret));

    assertThrows(IllegalArgumentException.class,
        () -> patroniSecret.buildSource(generatorContext));
  }

  @Test
  void generateResourcesWithReplicateFromSecretAndNoAuthenticatorPassword_shouldThrowAnException() {
    cluster.getSpec().setReplicateFrom(new StackGresClusterReplicateFrom());
    cluster.getSpec().getReplicateFrom().setInstance(new StackGresClusterReplicateFromInstance());
    cluster.getSpec().getReplicateFrom().getInstance()
        .setExternal(new StackGresClusterReplicateFromExternal());
    cluster.getSpec().getReplicateFrom().getInstance().getExternal()
        .setHost("test");
    cluster.getSpec().getReplicateFrom().getInstance().getExternal()
        .setPort(5433);
    cluster.getSpec().getReplicateFrom()
        .setUsers(new StackGresClusterReplicateFromUsers());
    cluster.getSpec().getReplicateFrom().getUsers()
        .setSuperuser(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getSuperuser().setUsername(new SecretKeySelector(SUPERUSER_USERNAME_ENV, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getSuperuser().setPassword(new SecretKeySelector(SUPERUSER_PASSWORD_KEY, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .setReplication(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getReplication().setUsername(new SecretKeySelector(REPLICATION_USERNAME_ENV, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getReplication().setPassword(new SecretKeySelector(REPLICATION_PASSWORD_KEY, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .setAuthenticator(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getAuthenticator().setUsername(new SecretKeySelector(AUTHENTICATOR_USERNAME_ENV, "test"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getAuthenticator().setPassword(new SecretKeySelector(AUTHENTICATOR_PASSWORD_KEY, "test"));
    when(generatorContext.getExternalSuperuserUsernameSecret())
        .thenReturn(Optional.of(existentSecret));
    when(generatorContext.getExternalSuperuserPasswordSecret())
        .thenReturn(Optional.of(existentSecret));
    when(generatorContext.getExternalReplicationUsernameSecret())
        .thenReturn(Optional.of(existentSecret));
    when(generatorContext.getExternalReplicationPasswordSecret())
        .thenReturn(Optional.of(existentSecret));
    when(generatorContext.getExternalAuthenticatorUsernameSecret())
        .thenReturn(Optional.of(existentSecret));

    assertThrows(IllegalArgumentException.class,
        () -> patroniSecret.buildSource(generatorContext));
  }

  @Test
  void generateResourcesForBabelfishFlavor_shouldGenerateRandomPasswords() {
    cluster.getSpec().getPostgres().setFlavor(StackGresPostgresFlavor.BABELFISH.toString());
    Secret secret = patroniSecret.buildSource(generatorContext);

    final Map<String, String> data = ResourceUtil.decodeSecret(secret.getData());
    assertTrue(data.containsKey(SUPERUSER_USERNAME_ENV));
    assertTrue(data.containsKey(SUPERUSER_PASSWORD_ENV));
    assertTrue(data.containsKey(SUPERUSER_PASSWORD_KEY));
    assertTrue(data.containsKey(REPLICATION_USERNAME_ENV));
    assertTrue(data.containsKey(REPLICATION_PASSWORD_ENV));
    assertTrue(data.containsKey(REPLICATION_PASSWORD_KEY));
    assertTrue(data.containsKey(AUTHENTICATOR_USERNAME_ENV));
    assertTrue(data.containsKey(AUTHENTICATOR_PASSWORD_ENV
        .replace(AUTHENTICATOR_USERNAME, data.get(AUTHENTICATOR_USERNAME_ENV))));
    assertTrue(data.containsKey(AUTHENTICATOR_OPTIONS_ENV
        .replace(AUTHENTICATOR_USERNAME, data.get(AUTHENTICATOR_USERNAME_ENV))));
    assertTrue(data.containsKey(AUTHENTICATOR_PASSWORD_KEY));
    assertTrue(data.containsKey(PGBOUNCER_ADMIN_PASSWORD_KEY));
    assertTrue(data.containsKey(PGBOUNCER_STATS_PASSWORD_KEY));
    assertTrue(data.containsKey(BABELFISH_PASSWORD_KEY));
    assertTrue(data.containsKey(BABELFISH_CREATE_USER_SQL_KEY));
    assertTrue(data.containsKey(RESTAPI_USERNAME_ENV));
    assertTrue(data.containsKey(RESTAPI_PASSWORD_ENV));
    assertTrue(data.containsKey(RESTAPI_PASSWORD_KEY));

    final Map<String, String> existentData = decodedExistentSecretData;
    assertNotEquals(existentData.get(SUPERUSER_USERNAME_ENV), data.get(SUPERUSER_USERNAME_ENV));
    assertNotEquals(existentData.get(SUPERUSER_PASSWORD_KEY), data.get(SUPERUSER_PASSWORD_ENV));
    assertNotEquals(existentData.get(SUPERUSER_PASSWORD_KEY), data.get(SUPERUSER_PASSWORD_ENV));
    assertNotEquals(existentData.get(REPLICATION_USERNAME_ENV), data.get(REPLICATION_USERNAME_ENV));
    assertNotEquals(existentData.get(REPLICATION_PASSWORD_KEY), data.get(REPLICATION_PASSWORD_ENV));
    assertNotEquals(existentData.get(REPLICATION_PASSWORD_KEY), data.get(REPLICATION_PASSWORD_KEY));
    assertNotEquals(
        existentData.get(AUTHENTICATOR_USERNAME_ENV), data.get(AUTHENTICATOR_USERNAME_ENV));
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

}

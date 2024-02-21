/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static io.stackgres.common.StringUtil.generateRandom;
import static io.stackgres.common.patroni.StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.AUTHENTICATOR_USERNAME;
import static io.stackgres.common.patroni.StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.AUTHENTICATOR_USERNAME_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.BABELFISH_CREATE_USER_SQL_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.BABELFISH_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.PGBOUNCER_ADMIN_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.PGBOUNCER_STATS_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.REPLICATION_PASSWORD_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.REPLICATION_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.REPLICATION_USERNAME_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.REPLICATION_USERNAME_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.RESTAPI_PASSWORD_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.RESTAPI_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.RESTAPI_USERNAME_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.RESTAPI_USERNAME_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_USERNAME_ENV;
import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_USERNAME_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
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
  private LabelFactoryForCluster<StackGresCluster> labelFactory;

  @Mock
  private StackGresClusterContext context;

  private final PatroniSecret patroniSecret = new PatroniSecret();

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    patroniSecret.setFactoryFactory(labelFactory);
    when(labelFactory.genericLabels(any(StackGresCluster.class))).thenReturn(ImmutableMap.of());

    when(context.getSource()).thenReturn(cluster);
    lenient().when(context.getGeneratedSuperuserPassword()).thenReturn(generateRandom());
    lenient().when(context.getGeneratedReplicationPassword()).thenReturn(generateRandom());
    lenient().when(context.getGeneratedAuthenticatorPassword()).thenReturn(generateRandom());
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
    assertTrue(data.containsKey(AUTHENTICATOR_PASSWORD_KEY));
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
    assertTrue(data.containsKey(AUTHENTICATOR_PASSWORD_KEY));
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
    assertTrue(data.containsKey(AUTHENTICATOR_PASSWORD_KEY));
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
    assertTrue(data.containsKey(AUTHENTICATOR_PASSWORD_KEY));
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

}

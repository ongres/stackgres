/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedClusterSecretTest {

  @Mock
  private LabelFactoryForShardedCluster labelFactory;

  @Mock
  private StackGresShardedClusterContext context;

  private ShardedClusterSecret shardedClusterSecret;

  private StackGresShardedCluster cluster;

  @BeforeEach
  void setUp() {
    shardedClusterSecret = new ShardedClusterSecret();
    shardedClusterSecret.setFactoryFactory(labelFactory);
    cluster = Fixtures.shardedCluster().loadDefault().get();

    when(context.getSource()).thenReturn(cluster);
    lenient().when(context.getShardedCluster()).thenReturn(cluster);
    lenient().when(labelFactory.genericLabels(cluster)).thenReturn(ImmutableMap.of());
    lenient().when(context.getDatabaseSecret()).thenReturn(Optional.empty());
    lenient().when(context.getSuperuserUsername()).thenReturn(Optional.empty());
    lenient().when(context.getSuperuserPassword()).thenReturn(Optional.of("superuser-pass"));
    lenient().when(context.getReplicationUsername()).thenReturn(Optional.empty());
    lenient().when(context.getReplicationPassword()).thenReturn(Optional.of("replication-pass"));
    lenient().when(context.getAuthenticatorUsername()).thenReturn(Optional.empty());
    lenient().when(context.getAuthenticatorPassword()).thenReturn(Optional.of("authenticator-pass"));
    lenient().when(context.getPatroniRestApiPassword()).thenReturn(Optional.of("restapi-pass"));
    lenient().when(context.getGeneratedPgBouncerAdminPassword()).thenReturn("pgbouncer-admin-pass");
    lenient().when(context.getGeneratedPgBouncerStatsPassword()).thenReturn("pgbouncer-stats-pass");
  }

  @Test
  void generateResource_shouldGenerateSecretWithSuperuserCredentials() {
    List<HasMetadata> resources = shardedClusterSecret.generateResource(context).toList();

    assertEquals(1, resources.size());
    Secret secret = (Secret) resources.getFirst();
    Map<String, String> data = ResourceUtil.decodeSecret(secret.getData());

    assertNotNull(data.get(StackGresPasswordKeys.SUPERUSER_USERNAME_KEY));
    assertNotNull(data.get(StackGresPasswordKeys.SUPERUSER_USERNAME_ENV));
    assertNotNull(data.get(StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY));
    assertNotNull(data.get(StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV));
    assertEquals("superuser-pass", data.get(StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY));
  }

  @Test
  void generateResource_shouldGenerateSecretWithReplicationCredentials() {
    List<HasMetadata> resources = shardedClusterSecret.generateResource(context).toList();

    Secret secret = (Secret) resources.getFirst();
    Map<String, String> data = ResourceUtil.decodeSecret(secret.getData());

    assertNotNull(data.get(StackGresPasswordKeys.REPLICATION_USERNAME_KEY));
    assertNotNull(data.get(StackGresPasswordKeys.REPLICATION_USERNAME_ENV));
    assertNotNull(data.get(StackGresPasswordKeys.REPLICATION_PASSWORD_KEY));
    assertNotNull(data.get(StackGresPasswordKeys.REPLICATION_PASSWORD_ENV));
    assertEquals("replication-pass", data.get(StackGresPasswordKeys.REPLICATION_PASSWORD_KEY));
  }

  @Test
  void generateResource_shouldGenerateSecretWithAuthenticatorCredentials() {
    List<HasMetadata> resources = shardedClusterSecret.generateResource(context).toList();

    Secret secret = (Secret) resources.getFirst();
    Map<String, String> data = ResourceUtil.decodeSecret(secret.getData());

    assertNotNull(data.get(StackGresPasswordKeys.AUTHENTICATOR_USERNAME_KEY));
    assertNotNull(data.get(StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV));
    assertNotNull(data.get(StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_KEY));
  }

  @Test
  void generateResource_whenBabelfishFlavor_shouldIncludeBabelfishPassword() {
    cluster.getSpec().getPostgres().setFlavor("babelfish");
    lenient().when(context.getGeneratedBabelfishPassword()).thenReturn("babelfish-pass");

    List<HasMetadata> resources = shardedClusterSecret.generateResource(context).toList();

    Secret secret = (Secret) resources.getFirst();
    Map<String, String> data = ResourceUtil.decodeSecret(secret.getData());

    assertNotNull(data.get(StackGresPasswordKeys.BABELFISH_PASSWORD_KEY));
    assertEquals("babelfish-pass", data.get(StackGresPasswordKeys.BABELFISH_PASSWORD_KEY));
    assertNotNull(data.get(StackGresPasswordKeys.BABELFISH_CREATE_USER_SQL_KEY));
  }

  @Test
  void generateResource_whenVanillaFlavor_shouldNotIncludeBabelfishPassword() {
    List<HasMetadata> resources = shardedClusterSecret.generateResource(context).toList();

    Secret secret = (Secret) resources.getFirst();
    Map<String, String> data = ResourceUtil.decodeSecret(secret.getData());

    assertFalse(data.containsKey(StackGresPasswordKeys.BABELFISH_PASSWORD_KEY));
  }

  @Test
  void generateResource_shouldAlwaysIncludePgBouncerCredentials() {
    List<HasMetadata> resources = shardedClusterSecret.generateResource(context).toList();

    Secret secret = (Secret) resources.getFirst();
    Map<String, String> data = ResourceUtil.decodeSecret(secret.getData());

    assertEquals(StackGresPasswordKeys.PGBOUNCER_ADMIN_USERNAME,
        data.get(StackGresPasswordKeys.PGBOUNCER_ADMIN_USERNAME_ENV));
    assertNotNull(data.get(StackGresPasswordKeys.PGBOUNCER_ADMIN_PASSWORD_KEY));
    assertEquals(StackGresPasswordKeys.PGBOUNCER_STATS_USERNAME,
        data.get(StackGresPasswordKeys.PGBOUNCER_STATS_USERNAME_ENV));
    assertNotNull(data.get(StackGresPasswordKeys.PGBOUNCER_STATS_PASSWORD_KEY));
  }

  @Test
  void generateResource_shouldIncludePatroniRestApiPassword() {
    List<HasMetadata> resources = shardedClusterSecret.generateResource(context).toList();

    Secret secret = (Secret) resources.getFirst();
    Map<String, String> data = ResourceUtil.decodeSecret(secret.getData());

    assertEquals(StackGresPasswordKeys.RESTAPI_USERNAME,
        data.get(StackGresPasswordKeys.RESTAPI_USERNAME_KEY));
    assertEquals("restapi-pass", data.get(StackGresPasswordKeys.RESTAPI_PASSWORD_KEY));
    assertNotNull(data.get(StackGresPasswordKeys.RESTAPI_PASSWORD_ENV));
  }

  @Test
  void generateResource_shouldHaveCorrectNameAndNamespace() {
    List<HasMetadata> resources = shardedClusterSecret.generateResource(context).toList();

    Secret secret = (Secret) resources.getFirst();
    assertEquals(cluster.getMetadata().getName(), secret.getMetadata().getName());
    assertEquals(cluster.getMetadata().getNamespace(), secret.getMetadata().getNamespace());
    assertEquals("Opaque", secret.getType());
  }

  @Test
  void generateResource_secretDataShouldContainMd5Sum() {
    List<HasMetadata> resources = shardedClusterSecret.generateResource(context).toList();

    Secret secret = (Secret) resources.getFirst();
    Map<String, String> data = ResourceUtil.decodeSecret(secret.getData());

    assertTrue(data.containsKey(StackGresUtil.MD5SUM_KEY));
    assertTrue(data.containsKey(StackGresUtil.MD5SUM_2_KEY));
  }

}

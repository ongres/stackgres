/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterCredentials;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniCredentials;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFrom;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromInstance;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromUserSecretKeyRef;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromUsers;
import io.stackgres.common.crd.sgcluster.StackGresClusterUserSecretKeyRef;
import io.stackgres.common.crd.sgcluster.StackGresClusterUsersCredentials;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterCredentialsContextAppenderTest {

  private ClusterCredentialsContextAppender contextAppender;

  private StackGresCluster cluster;

  @Spy
  private StackGresClusterContext.Builder contextBuilder;

  @Mock
  private ResourceFinder<Secret> secretFinder;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    contextAppender = new ClusterCredentialsContextAppender(secretFinder);
  }

  @Test
  void givenClusterWithReplicateFromUsers_shouldReadUsersSecrets() {
    mockReplicateFromUsers();

    contextAppender.appendContext(cluster, contextBuilder);

    verify(secretFinder, times(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromUsersWithoutSecretForSuperuserUsername_shouldFail() {
    mockReplicateFromUsersWithoutSecret(StackGresPasswordKeys.SUPERUSER_USERNAME_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Superuser username secret missing-test-secret was not found", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromUsersWithoutSecretForSuperuserPassword_shouldFail() {
    mockReplicateFromUsersWithoutSecret(StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Superuser password secret missing-test-secret was not found", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromUsersWithoutSecretForReplicationUsername_shouldFail() {
    mockReplicateFromUsersWithoutSecret(StackGresPasswordKeys.REPLICATION_USERNAME_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Replication username secret missing-test-secret was not found", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromUsersWithoutSecretForReplicationPassword_shouldFail() {
    mockReplicateFromUsersWithoutSecret(StackGresPasswordKeys.REPLICATION_PASSWORD_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Replication password secret missing-test-secret was not found", ex.getMessage());

    verify(secretFinder, times(4)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromUsersWithoutSecretForAuthenticatorUsername_shouldFail() {
    mockReplicateFromUsersWithoutSecret(StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Authenticator username secret missing-test-secret was not found", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromUsersWithoutSecretForAuthenticatorPassword_shouldFail() {
    mockReplicateFromUsersWithoutSecret(StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Authenticator password secret missing-test-secret was not found", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromUsersWithoutKeyForSuperuserUsername_shouldFail() {
    mockReplicateFromUsersWithoutKey(StackGresPasswordKeys.SUPERUSER_USERNAME_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Superuser username key PATRONI_SUPERUSER_USERNAME"
        + " was not found in secret test-secret", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromUsersWithoutKeyForSuperuserPassword_shouldFail() {
    mockReplicateFromUsersWithoutKey(StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Superuser password key PATRONI_SUPERUSER_PASSWORD"
        + " was not found in secret test-secret", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromUsersWithoutKeyForReplicationUsername_shouldFail() {
    mockReplicateFromUsersWithoutKey(StackGresPasswordKeys.REPLICATION_USERNAME_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Replication username key PATRONI_REPLICATION_USERNAME"
        + " was not found in secret test-secret", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromUsersWithoutKeyForReplicationPassword_shouldFail() {
    mockReplicateFromUsersWithoutKey(StackGresPasswordKeys.REPLICATION_PASSWORD_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Replication password key PATRONI_REPLICATION_PASSWORD"
        + " was not found in secret test-secret", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromUsersWithoutKeyForAuthenticatorUsername_shouldFail() {
    mockReplicateFromUsersWithoutKey(StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Authenticator username key PATRONI_authenticator_USERNAME"
        + " was not found in secret test-secret", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromUsersWithoutKeyForAuthenticatorPassword_shouldFail() {
    mockReplicateFromUsersWithoutKey(StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Authenticator password key PATRONI_authenticator_PASSWORD"
        + " was not found in secret test-secret", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromCluster_shouldReadUsersSecrets() {
    mockReplicateFromCluster();

    contextAppender.appendContext(cluster, contextBuilder);

    verify(secretFinder, times(1)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromClusterWithoutSecret_shouldFail() {
    mockReplicateFromClusterWithoutSecret();

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Can not find secret test"
        + " for SGCluster test"
        + " to replicate from", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromClusterWithoutSecretForSuperuserUsername_shouldFail() {
    mockReplicateFromClusterWithoutKey(StackGresPasswordKeys.SUPERUSER_USERNAME_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Superuser username key PATRONI_SUPERUSER_USERNAME"
        + " was not found in secret test", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromClusterWithoutSecretForSuperuserPassword_shouldFail() {
    mockReplicateFromClusterWithoutKey(StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Superuser password key PATRONI_SUPERUSER_PASSWORD"
        + " was not found in secret test", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromClusterWithoutSecretForReplicationUsername_shouldFail() {
    mockReplicateFromClusterWithoutKey(StackGresPasswordKeys.REPLICATION_USERNAME_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Replication username key PATRONI_REPLICATION_USERNAME"
        + " was not found in secret test", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromClusterWithoutSecretForReplicationPassword_shouldFail() {
    mockReplicateFromClusterWithoutKey(StackGresPasswordKeys.REPLICATION_PASSWORD_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Replication password key PATRONI_REPLICATION_PASSWORD"
        + " was not found in secret test", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromClusterWithoutSecretForAuthenticatorUsername_shouldFail() {
    mockReplicateFromClusterWithoutKey(StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Authenticator username key PATRONI_authenticator_USERNAME"
        + " was not found in secret test", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromClusterWithoutSecretForAuthenticatorPassword_shouldFail() {
    mockReplicateFromClusterWithoutKey(StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Authenticator password key PATRONI_authenticator_PASSWORD"
        + " was not found in secret test", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentials_shouldReadUsersSecrets() {
    mockUsersCredentials();

    contextAppender.appendContext(cluster, contextBuilder);

    verify(secretFinder, times(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutSecretForSuperuserUsername_shouldFail() {
    mockUsersCredentialsWithoutSecret(StackGresPasswordKeys.SUPERUSER_USERNAME_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Superuser username secret missing-test-secret was not found", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutSecretForSuperuserPassword_shouldFail() {
    mockUsersCredentialsWithoutSecret(StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Superuser password secret missing-test-secret was not found", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutSecretForReplicationUsername_shouldFail() {
    mockUsersCredentialsWithoutSecret(StackGresPasswordKeys.REPLICATION_USERNAME_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Replication username secret missing-test-secret was not found", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutSecretForReplicationPassword_shouldFail() {
    mockUsersCredentialsWithoutSecret(StackGresPasswordKeys.REPLICATION_PASSWORD_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Replication password secret missing-test-secret was not found", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutSecretForAuthenticatorUsername_shouldFail() {
    mockUsersCredentialsWithoutSecret(StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Authenticator username secret missing-test-secret was not found", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutSecretForAuthenticatorPassword_shouldFail() {
    mockUsersCredentialsWithoutSecret(StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Authenticator password secret missing-test-secret was not found", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutKeyForSuperuserUsername_shouldFail() {
    mockUsersCredentialsWithoutKey(StackGresPasswordKeys.SUPERUSER_USERNAME_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Superuser username key PATRONI_SUPERUSER_USERNAME"
        + " was not found in secret test-secret", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutKeyForSuperuserPassword_shouldFail() {
    mockUsersCredentialsWithoutKey(StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Superuser password key PATRONI_SUPERUSER_PASSWORD"
        + " was not found in secret test-secret", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutKeyForReplicationUsername_shouldFail() {
    mockUsersCredentialsWithoutKey(StackGresPasswordKeys.REPLICATION_USERNAME_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Replication username key PATRONI_REPLICATION_USERNAME"
        + " was not found in secret test-secret", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutKeyForReplicationPassword_shouldFail() {
    mockUsersCredentialsWithoutKey(StackGresPasswordKeys.REPLICATION_PASSWORD_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Replication password key PATRONI_REPLICATION_PASSWORD"
        + " was not found in secret test-secret", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutKeyForAuthenticatorUsername_shouldFail() {
    mockUsersCredentialsWithoutKey(StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Authenticator username key PATRONI_authenticator_USERNAME"
        + " was not found in secret test-secret", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutKeyForAuthenticatorPassword_shouldFail() {
    mockUsersCredentialsWithoutKey(StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Authenticator password key PATRONI_authenticator_PASSWORD"
        + " was not found in secret test-secret", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithPatroniCredentials_shouldReadpatroniSecrets() {
    mockPatroniCredentials();

    contextAppender.appendContext(cluster, contextBuilder);

    verify(secretFinder, times(1)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithPatroniCredentialsWithoutSecretForRestApiPassword_shouldFail() {
    mockPatroniCredentialsWithoutSecret(StackGresPasswordKeys.RESTAPI_PASSWORD_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Patroni REST API password secret missing-test-secret was not found", ex.getMessage());

    verify(secretFinder, times(1)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithPatroniCredentialsWithoutKeyForRestApiPassword_shouldFail() {
    mockPatroniCredentialsWithoutKey(StackGresPasswordKeys.RESTAPI_PASSWORD_ENV);

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Patroni REST API password key PATRONI_RESTAPI_PASSWORD"
        + " was not found in secret test-secret", ex.getMessage());

    verify(secretFinder, times(1)).findByNameAndNamespace(any(), any());
  }

  private void mockReplicateFromUsers() {
    cluster.getSpec().setReplicateFrom(new StackGresClusterReplicateFrom());
    cluster.getSpec().getReplicateFrom().setUsers(new StackGresClusterReplicateFromUsers());
    cluster.getSpec().getReplicateFrom().getUsers()
        .setSuperuser(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getSuperuser().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.SUPERUSER_USERNAME_ENV, "test-secret"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getSuperuser().setPassword(new SecretKeySelector(
            StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV, "test-secret"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .setReplication(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getReplication().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.REPLICATION_USERNAME_ENV, "test-secret"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getReplication().setPassword(new SecretKeySelector(
            StackGresPasswordKeys.REPLICATION_PASSWORD_ENV, "test-secret"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .setAuthenticator(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getAuthenticator().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV, "test-secret"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getAuthenticator().setPassword(new SecretKeySelector(
            StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV, "test-secret"));
    when(secretFinder.findByNameAndNamespace(
        "test-secret",
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(new SecretBuilder()
            .withData(Map.ofEntries(
                Map.entry(
                    StackGresPasswordKeys.SUPERUSER_USERNAME_ENV,
                    ResourceUtil.encodeSecret("postgres")),
                Map.entry(
                    StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("postgres")),
                Map.entry(
                    StackGresPasswordKeys.REPLICATION_USERNAME_ENV,
                    ResourceUtil.encodeSecret("replicator")),
                Map.entry(
                    StackGresPasswordKeys.REPLICATION_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("replicator")),
                Map.entry(
                    StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV,
                    ResourceUtil.encodeSecret("authenticator")),
                Map.entry(
                    StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("authenticator"))))
            .build()));
  }

  private void mockReplicateFromUsersWithoutSecret(String key) {
    cluster.getSpec().setReplicateFrom(new StackGresClusterReplicateFrom());
    cluster.getSpec().getReplicateFrom().setUsers(new StackGresClusterReplicateFromUsers());
    cluster.getSpec().getReplicateFrom().getUsers()
        .setSuperuser(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getSuperuser().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.SUPERUSER_USERNAME_ENV,
            key.equals(StackGresPasswordKeys.SUPERUSER_USERNAME_ENV)
            ? "missing-test-secret" : "test-secret"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getSuperuser().setPassword(new SecretKeySelector(
            StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV,
            key.equals(StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV)
            ? "missing-test-secret" : "test-secret"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .setReplication(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getReplication().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.REPLICATION_USERNAME_ENV,
            key.equals(StackGresPasswordKeys.REPLICATION_USERNAME_ENV)
            ? "missing-test-secret" : "test-secret"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getReplication().setPassword(new SecretKeySelector(
            StackGresPasswordKeys.REPLICATION_PASSWORD_ENV,
            key.equals(StackGresPasswordKeys.REPLICATION_PASSWORD_ENV)
            ? "missing-test-secret" : "test-secret"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .setAuthenticator(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getAuthenticator().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV,
            key.equals(StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV)
            ? "missing-test-secret" : "test-secret"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getAuthenticator().setPassword(new SecretKeySelector(
            StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV,
            key.equals(StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV)
            ? "missing-test-secret" : "test-secret"));
    when(secretFinder.findByNameAndNamespace(
        "missing-test-secret",
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.empty());
    Mockito.lenient()
        .when(secretFinder.findByNameAndNamespace(
            "test-secret",
            cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(new SecretBuilder()
            .withData(Map.ofEntries(
                Map.entry(
                    StackGresPasswordKeys.SUPERUSER_USERNAME_ENV,
                    ResourceUtil.encodeSecret("postgres")),
                Map.entry(
                    StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("postgres")),
                Map.entry(
                    StackGresPasswordKeys.REPLICATION_USERNAME_ENV,
                    ResourceUtil.encodeSecret("replicator")),
                Map.entry(
                    StackGresPasswordKeys.REPLICATION_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("replicator")),
                Map.entry(
                    StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV,
                    ResourceUtil.encodeSecret("authenticator")),
                Map.entry(
                    StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("authenticator"))))
            .build()));
  }

  private void mockReplicateFromUsersWithoutKey(String key) {
    cluster.getSpec().setReplicateFrom(new StackGresClusterReplicateFrom());
    cluster.getSpec().getReplicateFrom().setUsers(new StackGresClusterReplicateFromUsers());
    cluster.getSpec().getReplicateFrom().getUsers()
        .setSuperuser(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getSuperuser().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.SUPERUSER_USERNAME_ENV, "test-secret"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getSuperuser().setPassword(new SecretKeySelector(
            StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV, "test-secret"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .setReplication(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getReplication().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.REPLICATION_USERNAME_ENV, "test-secret"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getReplication().setPassword(new SecretKeySelector(
            StackGresPasswordKeys.REPLICATION_PASSWORD_ENV, "test-secret"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .setAuthenticator(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getAuthenticator().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV, "test-secret"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getAuthenticator().setPassword(new SecretKeySelector(
            StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV, "test-secret"));
    when(secretFinder.findByNameAndNamespace(
        "test-secret",
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(new SecretBuilder()
            .withData(Map.ofEntries(
                Map.entry(
                    key.equals(StackGresPasswordKeys.SUPERUSER_USERNAME_ENV)
                    ? "missing-key" : StackGresPasswordKeys.SUPERUSER_USERNAME_ENV,
                    ResourceUtil.encodeSecret("postgres")),
                Map.entry(
                    key.equals(StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV)
                    ? "missing-key" : StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("postgres")),
                Map.entry(
                    key.equals(StackGresPasswordKeys.REPLICATION_USERNAME_ENV)
                    ? "missing-key" : StackGresPasswordKeys.REPLICATION_USERNAME_ENV,
                    ResourceUtil.encodeSecret("replicator")),
                Map.entry(
                    key.equals(StackGresPasswordKeys.REPLICATION_PASSWORD_ENV)
                    ? "missing-key" : StackGresPasswordKeys.REPLICATION_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("replicator")),
                Map.entry(
                    key.equals(StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV)
                    ? "missing-key" : StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV,
                    ResourceUtil.encodeSecret("authenticator")),
                Map.entry(
                    key.equals(StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV)
                    ? "missing-key" : StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("authenticator"))))
            .build()));
  }

  private void mockReplicateFromCluster() {
    StackGresCluster replicatedCluster = new StackGresCluster();
    replicatedCluster.setMetadata(new ObjectMeta());
    replicatedCluster.getMetadata().setName("test");
    cluster.getSpec().setReplicateFrom(new StackGresClusterReplicateFrom());
    cluster.getSpec().getReplicateFrom().setInstance(new StackGresClusterReplicateFromInstance());
    cluster.getSpec().getReplicateFrom().getInstance()
        .setSgCluster("test");
    when(secretFinder.findByNameAndNamespace(
        "test",
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(new SecretBuilder()
            .withData(Map.ofEntries(
                Map.entry(
                    StackGresPasswordKeys.SUPERUSER_USERNAME_ENV,
                    ResourceUtil.encodeSecret("postgres")),
                Map.entry(
                    StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("postgres")),
                Map.entry(
                    StackGresPasswordKeys.REPLICATION_USERNAME_ENV,
                    ResourceUtil.encodeSecret("replicator")),
                Map.entry(
                    StackGresPasswordKeys.REPLICATION_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("replicator")),
                Map.entry(
                    StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV,
                    ResourceUtil.encodeSecret("authenticator")),
                Map.entry(
                    StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("authenticator"))))
            .build()));
  }

  private void mockReplicateFromClusterWithoutSecret() {
    cluster.getSpec().setReplicateFrom(new StackGresClusterReplicateFrom());
    cluster.getSpec().getReplicateFrom().setInstance(new StackGresClusterReplicateFromInstance());
    cluster.getSpec().getReplicateFrom().getInstance()
        .setSgCluster("test");
    when(secretFinder.findByNameAndNamespace(
        "test",
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.empty());
  }

  private void mockReplicateFromClusterWithoutKey(String key) {
    cluster.getSpec().setReplicateFrom(new StackGresClusterReplicateFrom());
    cluster.getSpec().getReplicateFrom().setInstance(new StackGresClusterReplicateFromInstance());
    cluster.getSpec().getReplicateFrom().getInstance()
        .setSgCluster("test");
    when(secretFinder.findByNameAndNamespace(
        "test",
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(new SecretBuilder()
            .withData(Map.ofEntries(
                Map.entry(
                    key.equals(StackGresPasswordKeys.SUPERUSER_USERNAME_ENV)
                    ? "missing-key" : StackGresPasswordKeys.SUPERUSER_USERNAME_ENV,
                    ResourceUtil.encodeSecret("postgres")),
                Map.entry(
                    key.equals(StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV)
                    ? "missing-key" : StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("postgres")),
                Map.entry(
                    key.equals(StackGresPasswordKeys.REPLICATION_USERNAME_ENV)
                    ? "missing-key" : StackGresPasswordKeys.REPLICATION_USERNAME_ENV,
                    ResourceUtil.encodeSecret("replicator")),
                Map.entry(
                    key.equals(StackGresPasswordKeys.REPLICATION_PASSWORD_ENV)
                    ? "missing-key" : StackGresPasswordKeys.REPLICATION_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("replicator")),
                Map.entry(
                    key.equals(StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV)
                    ? "missing-key" : StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV,
                    ResourceUtil.encodeSecret("authenticator")),
                Map.entry(
                    key.equals(StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV)
                    ? "missing-key" : StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("authenticator"))))
            .build()));
  }

  private void mockUsersCredentials() {
    cluster.getSpec().getConfigurations().setCredentials(new StackGresClusterCredentials());
    cluster.getSpec().getConfigurations().getCredentials()
        .setUsers(new StackGresClusterUsersCredentials());
    cluster.getSpec().getConfigurations().getCredentials().getUsers()
        .setSuperuser(new StackGresClusterUserSecretKeyRef());
    cluster.getSpec().getConfigurations().getCredentials().getUsers()
        .getSuperuser().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.SUPERUSER_USERNAME_ENV, "test-secret"));
    cluster.getSpec().getConfigurations().getCredentials().getUsers()
        .getSuperuser().setPassword(new SecretKeySelector(
            StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV, "test-secret"));
    cluster.getSpec().getConfigurations().getCredentials().getUsers()
        .setReplication(new StackGresClusterUserSecretKeyRef());
    cluster.getSpec().getConfigurations().getCredentials().getUsers()
        .getReplication().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.REPLICATION_USERNAME_ENV, "test-secret"));
    cluster.getSpec().getConfigurations().getCredentials().getUsers()
        .getReplication().setPassword(new SecretKeySelector(
            StackGresPasswordKeys.REPLICATION_PASSWORD_ENV, "test-secret"));
    cluster.getSpec().getConfigurations().getCredentials().getUsers()
        .setAuthenticator(new StackGresClusterUserSecretKeyRef());
    cluster.getSpec().getConfigurations().getCredentials().getUsers()
        .getAuthenticator().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV, "test-secret"));
    cluster.getSpec().getConfigurations().getCredentials().getUsers()
        .getAuthenticator().setPassword(new SecretKeySelector(
            StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV, "test-secret"));
    when(secretFinder.findByNameAndNamespace(
        "test-secret",
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(new SecretBuilder()
            .withData(Map.ofEntries(
                Map.entry(
                    StackGresPasswordKeys.SUPERUSER_USERNAME_ENV,
                    ResourceUtil.encodeSecret("postgres")),
                Map.entry(
                    StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("postgres")),
                Map.entry(
                    StackGresPasswordKeys.REPLICATION_USERNAME_ENV,
                    ResourceUtil.encodeSecret("replicator")),
                Map.entry(
                    StackGresPasswordKeys.REPLICATION_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("replicator")),
                Map.entry(
                    StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV,
                    ResourceUtil.encodeSecret("authenticator")),
                Map.entry(
                    StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("authenticator"))))
            .build()));
  }

  private void mockUsersCredentialsWithoutSecret(String key) {
    cluster.getSpec().getConfigurations().setCredentials(new StackGresClusterCredentials());
    cluster.getSpec().getConfigurations().getCredentials()
        .setUsers(new StackGresClusterUsersCredentials());
    cluster.getSpec().getConfigurations().getCredentials().getUsers()
        .setSuperuser(new StackGresClusterUserSecretKeyRef());
    cluster.getSpec().getConfigurations().getCredentials().getUsers()
        .getSuperuser().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.SUPERUSER_USERNAME_ENV,
            key.equals(StackGresPasswordKeys.SUPERUSER_USERNAME_ENV)
            ? "missing-test-secret" : "test-secret"));
    cluster.getSpec().getConfigurations().getCredentials().getUsers()
        .getSuperuser().setPassword(new SecretKeySelector(
            StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV,
            key.equals(StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV)
            ? "missing-test-secret" : "test-secret"));
    cluster.getSpec().getConfigurations().getCredentials().getUsers()
        .setReplication(new StackGresClusterUserSecretKeyRef());
    cluster.getSpec().getConfigurations().getCredentials().getUsers()
        .getReplication().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.REPLICATION_USERNAME_ENV,
            key.equals(StackGresPasswordKeys.REPLICATION_USERNAME_ENV)
            ? "missing-test-secret" : "test-secret"));
    cluster.getSpec().getConfigurations().getCredentials().getUsers()
        .getReplication().setPassword(new SecretKeySelector(
            StackGresPasswordKeys.REPLICATION_PASSWORD_ENV,
            key.equals(StackGresPasswordKeys.REPLICATION_PASSWORD_ENV)
            ? "missing-test-secret" : "test-secret"));
    cluster.getSpec().getConfigurations().getCredentials().getUsers()
        .setAuthenticator(new StackGresClusterUserSecretKeyRef());
    cluster.getSpec().getConfigurations().getCredentials().getUsers()
        .getAuthenticator().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV,
            key.equals(StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV)
            ? "missing-test-secret" : "test-secret"));
    cluster.getSpec().getConfigurations().getCredentials().getUsers()
        .getAuthenticator().setPassword(new SecretKeySelector(
            StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV,
            key.equals(StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV)
            ? "missing-test-secret" : "test-secret"));
    when(secretFinder.findByNameAndNamespace(
        "missing-test-secret",
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.empty());
    Mockito.lenient()
        .when(secretFinder.findByNameAndNamespace(
            "test-secret",
            cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(new SecretBuilder()
            .withData(Map.ofEntries(
                Map.entry(
                    StackGresPasswordKeys.SUPERUSER_USERNAME_ENV,
                    ResourceUtil.encodeSecret("postgres")),
                Map.entry(
                    StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("postgres")),
                Map.entry(
                    StackGresPasswordKeys.REPLICATION_USERNAME_ENV,
                    ResourceUtil.encodeSecret("replicator")),
                Map.entry(
                    StackGresPasswordKeys.REPLICATION_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("replicator")),
                Map.entry(
                    StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV,
                    ResourceUtil.encodeSecret("authenticator")),
                Map.entry(
                    StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("authenticator"))))
            .build()));
  }

  private void mockUsersCredentialsWithoutKey(String key) {
    cluster.getSpec().getConfigurations().setCredentials(new StackGresClusterCredentials());
    cluster.getSpec().getConfigurations().getCredentials()
        .setUsers(new StackGresClusterUsersCredentials());
    cluster.getSpec().getConfigurations().getCredentials().getUsers()
        .setSuperuser(new StackGresClusterUserSecretKeyRef());
    cluster.getSpec().getConfigurations().getCredentials().getUsers()
        .getSuperuser().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.SUPERUSER_USERNAME_ENV, "test-secret"));
    cluster.getSpec().getConfigurations().getCredentials().getUsers()
        .getSuperuser().setPassword(new SecretKeySelector(
            StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV, "test-secret"));
    cluster.getSpec().getConfigurations().getCredentials().getUsers()
        .setReplication(new StackGresClusterUserSecretKeyRef());
    cluster.getSpec().getConfigurations().getCredentials().getUsers()
        .getReplication().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.REPLICATION_USERNAME_ENV, "test-secret"));
    cluster.getSpec().getConfigurations().getCredentials().getUsers()
        .getReplication().setPassword(new SecretKeySelector(
            StackGresPasswordKeys.REPLICATION_PASSWORD_ENV, "test-secret"));
    cluster.getSpec().getConfigurations().getCredentials().getUsers()
        .setAuthenticator(new StackGresClusterUserSecretKeyRef());
    cluster.getSpec().getConfigurations().getCredentials().getUsers()
        .getAuthenticator().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV, "test-secret"));
    cluster.getSpec().getConfigurations().getCredentials().getUsers()
        .getAuthenticator().setPassword(new SecretKeySelector(
            StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV, "test-secret"));
    when(secretFinder.findByNameAndNamespace(
        "test-secret",
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(new SecretBuilder()
            .withData(Map.ofEntries(
                Map.entry(
                    key.equals(StackGresPasswordKeys.SUPERUSER_USERNAME_ENV)
                    ? "missing-key" : StackGresPasswordKeys.SUPERUSER_USERNAME_ENV,
                    ResourceUtil.encodeSecret("postgres")),
                Map.entry(
                    key.equals(StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV)
                    ? "missing-key" : StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("postgres")),
                Map.entry(
                    key.equals(StackGresPasswordKeys.REPLICATION_USERNAME_ENV)
                    ? "missing-key" : StackGresPasswordKeys.REPLICATION_USERNAME_ENV,
                    ResourceUtil.encodeSecret("replicator")),
                Map.entry(
                    key.equals(StackGresPasswordKeys.REPLICATION_PASSWORD_ENV)
                    ? "missing-key" : StackGresPasswordKeys.REPLICATION_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("replicator")),
                Map.entry(
                    key.equals(StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV)
                    ? "missing-key" : StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV,
                    ResourceUtil.encodeSecret("authenticator")),
                Map.entry(
                    key.equals(StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV)
                    ? "missing-key" : StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("authenticator"))))
            .build()));
  }

  private void mockPatroniCredentials() {
    cluster.getSpec().getConfigurations().setCredentials(new StackGresClusterCredentials());
    cluster.getSpec().getConfigurations().getCredentials()
        .setPatroni(new StackGresClusterPatroniCredentials());
    cluster.getSpec().getConfigurations().getCredentials().getPatroni()
        .setRestApiPassword(new SecretKeySelector(
            StackGresPasswordKeys.RESTAPI_PASSWORD_ENV, "test-secret"));
    when(secretFinder.findByNameAndNamespace(
        "test-secret",
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(new SecretBuilder()
            .withData(Map.ofEntries(
                Map.entry(
                    StackGresPasswordKeys.RESTAPI_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("patroni"))))
            .build()));
  }

  private void mockPatroniCredentialsWithoutSecret(String key) {
    cluster.getSpec().getConfigurations().setCredentials(new StackGresClusterCredentials());
    cluster.getSpec().getConfigurations().getCredentials()
        .setPatroni(new StackGresClusterPatroniCredentials());
    cluster.getSpec().getConfigurations().getCredentials().getPatroni()
        .setRestApiPassword(new SecretKeySelector(
            StackGresPasswordKeys.RESTAPI_PASSWORD_ENV,
            key.equals(StackGresPasswordKeys.RESTAPI_PASSWORD_ENV)
            ? "missing-test-secret" : "test-secret"));
    when(secretFinder.findByNameAndNamespace(
        "missing-test-secret",
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.empty());
  }

  private void mockPatroniCredentialsWithoutKey(String key) {
    cluster.getSpec().getConfigurations().setCredentials(new StackGresClusterCredentials());
    cluster.getSpec().getConfigurations().getCredentials()
        .setPatroni(new StackGresClusterPatroniCredentials());
    cluster.getSpec().getConfigurations().getCredentials().getPatroni()
        .setRestApiPassword(new SecretKeySelector(
            StackGresPasswordKeys.RESTAPI_PASSWORD_ENV, "test-secret"));
    when(secretFinder.findByNameAndNamespace(
        "test-secret",
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(new SecretBuilder()
            .withData(Map.ofEntries(
                Map.entry(
                    key.equals(StackGresPasswordKeys.RESTAPI_PASSWORD_ENV)
                    ? "missing-key" : StackGresPasswordKeys.RESTAPI_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("patroni"))))
            .build()));
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresClusterCredentials;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniCredentials;
import io.stackgres.common.crd.sgcluster.StackGresClusterUserSecretKeyRef;
import io.stackgres.common.crd.sgcluster.StackGresClusterUsersCredentials;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterConfigurations;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ShardedClusterRequiredResourcesGeneratorForCredentialsTest
    extends AbstractShardedClusterRequiredResourcesGeneratorTest {

  @Test
  void givenClusterWithUsersCredentials_shouldReadUsersSecrets() {
    mockUsersCredentials();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(instanceProfile));

    generator.getRequiredResources(cluster);

    verify(postgresConfigFinder, times(3)).findByNameAndNamespace(any(), any());
    verify(poolingConfigFinder, times(2)).findByNameAndNamespace(any(), any());
    verify(profileConfigFinder, times(2)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutSecretForSuperuserUsername_shouldFail() {
    mockUsersCredentialsWithoutSecret(StackGresPasswordKeys.SUPERUSER_USERNAME_ENV);
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(instanceProfile));

    assertException("Superuser username secret missing-test-secret was not found");

    verify(postgresConfigFinder, times(3)).findByNameAndNamespace(any(), any());
    verify(poolingConfigFinder, times(2)).findByNameAndNamespace(any(), any());
    verify(profileConfigFinder, times(2)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutSecretForSuperuserPassword_shouldFail() {
    mockUsersCredentialsWithoutSecret(StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV);
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(instanceProfile));

    assertException("Superuser password secret missing-test-secret was not found");

    verify(postgresConfigFinder, times(3)).findByNameAndNamespace(any(), any());
    verify(poolingConfigFinder, times(2)).findByNameAndNamespace(any(), any());
    verify(profileConfigFinder, times(2)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutSecretForReplicationUsername_shouldFail() {
    mockUsersCredentialsWithoutSecret(StackGresPasswordKeys.REPLICATION_USERNAME_ENV);
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(instanceProfile));

    assertException("Replication username secret missing-test-secret was not found");

    verify(postgresConfigFinder, times(3)).findByNameAndNamespace(any(), any());
    verify(poolingConfigFinder, times(2)).findByNameAndNamespace(any(), any());
    verify(profileConfigFinder, times(2)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutSecretForReplicationPassword_shouldFail() {
    mockUsersCredentialsWithoutSecret(StackGresPasswordKeys.REPLICATION_PASSWORD_ENV);
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(instanceProfile));

    assertException("Replication password secret missing-test-secret was not found");

    verify(postgresConfigFinder, times(3)).findByNameAndNamespace(any(), any());
    verify(poolingConfigFinder, times(2)).findByNameAndNamespace(any(), any());
    verify(profileConfigFinder, times(2)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutSecretForAuthenticatorUsername_shouldFail() {
    mockUsersCredentialsWithoutSecret(StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV);
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(instanceProfile));

    assertException("Authenticator username secret missing-test-secret was not found");

    verify(postgresConfigFinder, times(3)).findByNameAndNamespace(any(), any());
    verify(poolingConfigFinder, times(2)).findByNameAndNamespace(any(), any());
    verify(profileConfigFinder, times(2)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutSecretForAuthenticatorPassword_shouldFail() {
    mockUsersCredentialsWithoutSecret(StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV);
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(instanceProfile));

    assertException("Authenticator password secret missing-test-secret was not found");

    verify(postgresConfigFinder, times(3)).findByNameAndNamespace(any(), any());
    verify(poolingConfigFinder, times(2)).findByNameAndNamespace(any(), any());
    verify(profileConfigFinder, times(2)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutKeyForSuperuserUsername_shouldFail() {
    mockUsersCredentialsWithoutKey(StackGresPasswordKeys.SUPERUSER_USERNAME_ENV);
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(instanceProfile));

    assertException("Superuser username key PATRONI_SUPERUSER_USERNAME"
        + " was not found in secret test-secret");

    verify(postgresConfigFinder, times(3)).findByNameAndNamespace(any(), any());
    verify(poolingConfigFinder, times(2)).findByNameAndNamespace(any(), any());
    verify(profileConfigFinder, times(2)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutKeyForSuperuserPassword_shouldFail() {
    mockUsersCredentialsWithoutKey(StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV);
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(instanceProfile));

    assertException("Superuser password key PATRONI_SUPERUSER_PASSWORD"
        + " was not found in secret test-secret");

    verify(postgresConfigFinder, times(3)).findByNameAndNamespace(any(), any());
    verify(poolingConfigFinder, times(2)).findByNameAndNamespace(any(), any());
    verify(profileConfigFinder, times(2)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutKeyForReplicationUsername_shouldFail() {
    mockUsersCredentialsWithoutKey(StackGresPasswordKeys.REPLICATION_USERNAME_ENV);
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(instanceProfile));

    assertException("Replication username key PATRONI_REPLICATION_USERNAME"
        + " was not found in secret test-secret");

    verify(postgresConfigFinder, times(3)).findByNameAndNamespace(any(), any());
    verify(poolingConfigFinder, times(2)).findByNameAndNamespace(any(), any());
    verify(profileConfigFinder, times(2)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutKeyForReplicationPassword_shouldFail() {
    mockUsersCredentialsWithoutKey(StackGresPasswordKeys.REPLICATION_PASSWORD_ENV);
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(instanceProfile));

    assertException("Replication password key PATRONI_REPLICATION_PASSWORD"
        + " was not found in secret test-secret");

    verify(postgresConfigFinder, times(3)).findByNameAndNamespace(any(), any());
    verify(poolingConfigFinder, times(2)).findByNameAndNamespace(any(), any());
    verify(profileConfigFinder, times(2)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutKeyForAuthenticatorUsername_shouldFail() {
    mockUsersCredentialsWithoutKey(StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV);
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(instanceProfile));

    assertException("Authenticator username key PATRONI_authenticator_USERNAME"
        + " was not found in secret test-secret");

    verify(postgresConfigFinder, times(3)).findByNameAndNamespace(any(), any());
    verify(poolingConfigFinder, times(2)).findByNameAndNamespace(any(), any());
    verify(profileConfigFinder, times(2)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutKeyForAuthenticatorPassword_shouldFail() {
    mockUsersCredentialsWithoutKey(StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV);
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(instanceProfile));

    assertException("Authenticator password key PATRONI_authenticator_PASSWORD"
        + " was not found in secret test-secret");

    verify(postgresConfigFinder, times(3)).findByNameAndNamespace(any(), any());
    verify(poolingConfigFinder, times(2)).findByNameAndNamespace(any(), any());
    verify(profileConfigFinder, times(2)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithPatroniCredentials_shouldReadpatroniSecrets() {
    mockPatroniCredentials();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(instanceProfile));

    generator.getRequiredResources(cluster);

    verify(postgresConfigFinder, times(3)).findByNameAndNamespace(any(), any());
    verify(poolingConfigFinder, times(2)).findByNameAndNamespace(any(), any());
    verify(profileConfigFinder, times(2)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithPatroniCredentialsWithoutSecretForRestApiPassword_shouldFail() {
    mockPatroniCredentialsWithoutSecret(StackGresPasswordKeys.RESTAPI_PASSWORD_ENV);
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(instanceProfile));

    assertException("Patroni REST API password secret missing-test-secret was not found");

    verify(postgresConfigFinder, times(3)).findByNameAndNamespace(any(), any());
    verify(poolingConfigFinder, times(2)).findByNameAndNamespace(any(), any());
    verify(profileConfigFinder, times(2)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithPatroniCredentialsWithoutKeyForRestApiPassword_shouldFail() {
    mockPatroniCredentialsWithoutKey(StackGresPasswordKeys.RESTAPI_PASSWORD_ENV);
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(instanceProfile));

    assertException("Patroni REST API password key PATRONI_RESTAPI_PASSWORD"
        + " was not found in secret test-secret");

    verify(postgresConfigFinder, times(3)).findByNameAndNamespace(any(), any());
    verify(poolingConfigFinder, times(2)).findByNameAndNamespace(any(), any());
    verify(profileConfigFinder, times(2)).findByNameAndNamespace(any(), any());
  }

  private void mockUsersCredentials() {
    cluster.getSpec().setConfigurations(new StackGresShardedClusterConfigurations());
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
    cluster.getSpec().setConfigurations(new StackGresShardedClusterConfigurations());
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
    cluster.getSpec().setConfigurations(new StackGresShardedClusterConfigurations());
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
    cluster.getSpec().setConfigurations(new StackGresShardedClusterConfigurations());
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
    cluster.getSpec().setConfigurations(new StackGresShardedClusterConfigurations());
    cluster.getSpec().getConfigurations().setCredentials(new StackGresClusterCredentials());
    cluster.getSpec().getConfigurations().getCredentials()
        .setPatroni(new StackGresClusterPatroniCredentials());
    cluster.getSpec().getConfigurations().getCredentials().getPatroni()
        .setRestApiPassword(new SecretKeySelector(
            StackGresPasswordKeys.RESTAPI_PASSWORD_ENV,
            key.equals(StackGresPasswordKeys.RESTAPI_PASSWORD_ENV)
            ? "missing-test-secret" : "test-secret"));
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

  private void mockPatroniCredentialsWithoutKey(String key) {
    cluster.getSpec().setConfigurations(new StackGresShardedClusterConfigurations());
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

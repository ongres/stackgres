/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterCredentials;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniCredentials;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFrom;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromInstance;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromStorage;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromUserSecretKeyRef;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromUsers;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterUserSecretKeyRef;
import io.stackgres.common.crd.sgcluster.StackGresClusterUsersCredentials;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ClusterRequiredResourcesGeneratorForReplicateFromTest
    extends AbstractClusterRequiredResourcesGeneratorTest {

  @Test
  void givenClusterWithReplicateFromUsers_shouldReadUsersSecrets() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromUsers();
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    generator.getRequiredResources(cluster);

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromUsersWithoutSecretForSuperuserUsername_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromUsersWithoutSecret(StackGresPasswordKeys.SUPERUSER_USERNAME_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Superuser username secret missing-test-secret was not found");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromUsersWithoutSecretForSuperuserPassword_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromUsersWithoutSecret(StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Superuser password secret missing-test-secret was not found");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromUsersWithoutSecretForReplicationUsername_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromUsersWithoutSecret(StackGresPasswordKeys.REPLICATION_USERNAME_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Replication username secret missing-test-secret was not found");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromUsersWithoutSecretForReplicationPassword_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromUsersWithoutSecret(StackGresPasswordKeys.REPLICATION_PASSWORD_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Replication password secret missing-test-secret was not found");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromUsersWithoutSecretForAuthenticatorUsername_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromUsersWithoutSecret(StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Authenticator username secret missing-test-secret was not found");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromUsersWithoutSecretForAuthenticatorPassword_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromUsersWithoutSecret(StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Authenticator password secret missing-test-secret was not found");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromUsersWithoutKeyForSuperuserUsername_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromUsersWithoutKey(StackGresPasswordKeys.SUPERUSER_USERNAME_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Superuser username key PATRONI_SUPERUSER_USERNAME"
        + " was not found in secret test-secret");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromUsersWithoutKeyForSuperuserPassword_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromUsersWithoutKey(StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Superuser password key PATRONI_SUPERUSER_PASSWORD"
        + " was not found in secret test-secret");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromUsersWithoutKeyForReplicationUsername_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromUsersWithoutKey(StackGresPasswordKeys.REPLICATION_USERNAME_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Replication username key PATRONI_REPLICATION_USERNAME"
        + " was not found in secret test-secret");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromUsersWithoutKeyForReplicationPassword_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromUsersWithoutKey(StackGresPasswordKeys.REPLICATION_PASSWORD_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Replication password key PATRONI_REPLICATION_PASSWORD"
        + " was not found in secret test-secret");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromUsersWithoutKeyForAuthenticatorUsername_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromUsersWithoutKey(StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Authenticator username key PATRONI_authenticator_USERNAME"
        + " was not found in secret test-secret");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromUsersWithoutKeyForAuthenticatorPassword_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromUsersWithoutKey(StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Authenticator password key PATRONI_authenticator_PASSWORD"
        + " was not found in secret test-secret");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromCluster_shouldReadUsersSecrets() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromCluster();
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    generator.getRequiredResources(cluster);

    verify(clusterFinder).findByNameAndNamespace(
        cluster.getSpec().getReplicateFrom().getInstance().getSgCluster(), clusterNamespace);
    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromClusterWithBackupConfig_shouldReadObjectStorage() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromClusterWithBackupConfig();
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    generator.getRequiredResources(cluster);

    verify(clusterFinder).findByNameAndNamespace(
        cluster.getSpec().getReplicateFrom().getInstance().getSgCluster(), clusterNamespace);
    verify(objectStorageFinder).findByNameAndNamespace(
        any(), any());
    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromClusterWithoutSecret_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromClusterWithoutSecret();
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Can not find secret test"
        + " for SGCluster test"
        + " to replicate from");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromClusterWithoutSecretForSuperuserUsername_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromClusterWithoutKey(StackGresPasswordKeys.SUPERUSER_USERNAME_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Superuser username key PATRONI_SUPERUSER_USERNAME"
        + " was not found in secret test");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromClusterWithoutSecretForSuperuserPassword_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromClusterWithoutKey(StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Superuser password key PATRONI_SUPERUSER_PASSWORD"
        + " was not found in secret test");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromClusterWithoutSecretForReplicationUsername_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromClusterWithoutKey(StackGresPasswordKeys.REPLICATION_USERNAME_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Replication username key PATRONI_REPLICATION_USERNAME"
        + " was not found in secret test");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromClusterWithoutSecretForReplicationPassword_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromClusterWithoutKey(StackGresPasswordKeys.REPLICATION_PASSWORD_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Replication password key PATRONI_REPLICATION_PASSWORD"
        + " was not found in secret test");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromClusterWithoutSecretForAuthenticatorUsername_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromClusterWithoutKey(StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Authenticator username key PATRONI_authenticator_USERNAME"
        + " was not found in secret test");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromClusterWithoutSecretForAuthenticatorPassword_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromClusterWithoutKey(StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Authenticator password key PATRONI_authenticator_PASSWORD"
        + " was not found in secret test");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromMissingCluster_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromMissingCluster();
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Can not find SGCluster test to replicate from");

    verify(clusterFinder).findByNameAndNamespace(
        cluster.getSpec().getReplicateFrom().getInstance().getSgCluster(), clusterNamespace);
    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromClusterWithMissingBackupConfig_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromClusterWithMissingBackupConfig();
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Can not find SGObjectStorage test to replicate from");

    verify(clusterFinder).findByNameAndNamespace(
        cluster.getSpec().getReplicateFrom().getInstance().getSgCluster(), clusterNamespace);
    verify(objectStorageFinder).findByNameAndNamespace(
        any(), any());
    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromStorage_shouldReadObjectStorage() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromStorage();
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    generator.getRequiredResources(cluster);

    verify(objectStorageFinder).findByNameAndNamespace(
        any(), any());
    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromMissingStorage_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromMissingStorage();
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Can not find SGObjectStorage test to replicate from");

    verify(objectStorageFinder).findByNameAndNamespace(
        any(), any());
    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentials_shouldReadUsersSecrets() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockUsersCredentials();
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    generator.getRequiredResources(cluster);

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutSecretForSuperuserUsername_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockUsersCredentialsWithoutSecret(StackGresPasswordKeys.SUPERUSER_USERNAME_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Superuser username secret missing-test-secret was not found");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutSecretForSuperuserPassword_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockUsersCredentialsWithoutSecret(StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Superuser password secret missing-test-secret was not found");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutSecretForReplicationUsername_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockUsersCredentialsWithoutSecret(StackGresPasswordKeys.REPLICATION_USERNAME_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Replication username secret missing-test-secret was not found");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutSecretForReplicationPassword_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockUsersCredentialsWithoutSecret(StackGresPasswordKeys.REPLICATION_PASSWORD_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Replication password secret missing-test-secret was not found");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutSecretForAuthenticatorUsername_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockUsersCredentialsWithoutSecret(StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Authenticator username secret missing-test-secret was not found");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutSecretForAuthenticatorPassword_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockUsersCredentialsWithoutSecret(StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Authenticator password secret missing-test-secret was not found");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutKeyForSuperuserUsername_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockUsersCredentialsWithoutKey(StackGresPasswordKeys.SUPERUSER_USERNAME_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Superuser username key PATRONI_SUPERUSER_USERNAME"
        + " was not found in secret test-secret");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutKeyForSuperuserPassword_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockUsersCredentialsWithoutKey(StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Superuser password key PATRONI_SUPERUSER_PASSWORD"
        + " was not found in secret test-secret");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutKeyForReplicationUsername_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockUsersCredentialsWithoutKey(StackGresPasswordKeys.REPLICATION_USERNAME_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Replication username key PATRONI_REPLICATION_USERNAME"
        + " was not found in secret test-secret");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutKeyForReplicationPassword_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockUsersCredentialsWithoutKey(StackGresPasswordKeys.REPLICATION_PASSWORD_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Replication password key PATRONI_REPLICATION_PASSWORD"
        + " was not found in secret test-secret");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutKeyForAuthenticatorUsername_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockUsersCredentialsWithoutKey(StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Authenticator username key PATRONI_authenticator_USERNAME"
        + " was not found in secret test-secret");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithUsersCredentialsWithoutKeyForAuthenticatorPassword_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockUsersCredentialsWithoutKey(StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Authenticator password key PATRONI_authenticator_PASSWORD"
        + " was not found in secret test-secret");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithPatroniCredentials_shouldReadpatroniSecrets() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockPatroniCredentials();
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    generator.getRequiredResources(cluster);

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithPatroniCredentialsWithoutSecretForRestApiPassword_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockPatroniCredentialsWithoutSecret(StackGresPasswordKeys.RESTAPI_PASSWORD_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Patroni REST API password secret missing-test-secret was not found");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithPatroniCredentialsWithoutKeyForRestApiPassword_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockPatroniCredentialsWithoutKey(StackGresPasswordKeys.RESTAPI_PASSWORD_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Patroni REST API password key PATRONI_RESTAPI_PASSWORD"
        + " was not found in secret test-secret");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
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
    when(clusterFinder.findByNameAndNamespace(
        "test",
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(replicatedCluster));
    when(secretFinder.findByNameAndNamespace(
        PatroniUtil.defaultReadWriteName("test"),
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
        PatroniUtil.defaultReadWriteName("test"),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.empty());
  }

  private void mockReplicateFromClusterWithoutKey(String key) {
    cluster.getSpec().setReplicateFrom(new StackGresClusterReplicateFrom());
    cluster.getSpec().getReplicateFrom().setInstance(new StackGresClusterReplicateFromInstance());
    cluster.getSpec().getReplicateFrom().getInstance()
        .setSgCluster("test");
    when(secretFinder.findByNameAndNamespace(
        PatroniUtil.defaultReadWriteName("test"),
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

  private void mockReplicateFromMissingCluster() {
    cluster.getSpec().setReplicateFrom(new StackGresClusterReplicateFrom());
    cluster.getSpec().getReplicateFrom().setInstance(new StackGresClusterReplicateFromInstance());
    cluster.getSpec().getReplicateFrom().getInstance()
        .setSgCluster("test");
    when(secretFinder.findByNameAndNamespace(
        PatroniUtil.defaultReadWriteName("test"),
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

  private void mockReplicateFromClusterWithBackupConfig() {
    StackGresCluster replicatedCluster = new StackGresCluster();
    replicatedCluster.setMetadata(new ObjectMeta());
    replicatedCluster.getMetadata().setName("test");
    replicatedCluster.setSpec(new StackGresClusterSpec());
    replicatedCluster.getSpec().setConfiguration(new StackGresClusterConfiguration());
    replicatedCluster.getSpec().getConfiguration()
        .setBackups(List.of(new StackGresClusterBackupConfiguration()));
    replicatedCluster.getSpec().getConfiguration().getBackups().get(0)
        .setObjectStorage("test");
    cluster.getSpec().setReplicateFrom(new StackGresClusterReplicateFrom());
    cluster.getSpec().getReplicateFrom().setInstance(new StackGresClusterReplicateFromInstance());
    cluster.getSpec().getReplicateFrom().getInstance()
        .setSgCluster("test");
    when(clusterFinder.findByNameAndNamespace(
        "test",
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(replicatedCluster));
    when(objectStorageFinder.findByNameAndNamespace(
        "test",
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(new StackGresObjectStorage()));
    when(secretFinder.findByNameAndNamespace(
        PatroniUtil.defaultReadWriteName("test"),
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

  private void mockReplicateFromClusterWithMissingBackupConfig() {
    cluster.getSpec().setReplicateFrom(new StackGresClusterReplicateFrom());
    cluster.getSpec().getReplicateFrom().setInstance(new StackGresClusterReplicateFromInstance());
    cluster.getSpec().getReplicateFrom().getInstance()
        .setSgCluster("test");
    StackGresCluster targetCluster = new StackGresCluster();
    targetCluster.setSpec(new StackGresClusterSpec());
    targetCluster.getSpec().setConfiguration(new StackGresClusterConfiguration());
    targetCluster.getSpec().getConfiguration()
        .setBackups(List.of(new StackGresClusterBackupConfiguration()));
    targetCluster.getSpec().getConfiguration().getBackups().get(0)
        .setObjectStorage("test");
    when(clusterFinder.findByNameAndNamespace(
        "test",
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(targetCluster));
    when(secretFinder.findByNameAndNamespace(
        PatroniUtil.defaultReadWriteName("test"),
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

  private void mockReplicateFromStorage() {
    cluster.getSpec().setReplicateFrom(new StackGresClusterReplicateFrom());
    cluster.getSpec().getReplicateFrom().setStorage(new StackGresClusterReplicateFromStorage());
    cluster.getSpec().getReplicateFrom().getStorage()
        .setSgObjectStorage("test");
    when(objectStorageFinder.findByNameAndNamespace(
        "test",
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(new StackGresObjectStorage()));
  }

  private void mockReplicateFromMissingStorage() {
    cluster.getSpec().setReplicateFrom(new StackGresClusterReplicateFrom());
    cluster.getSpec().getReplicateFrom().setStorage(new StackGresClusterReplicateFromStorage());
    cluster.getSpec().getReplicateFrom().getStorage()
        .setSgObjectStorage("test");
  }

  private void mockUsersCredentials() {
    cluster.getSpec().getConfiguration().setCredentials(new StackGresClusterCredentials());
    cluster.getSpec().getConfiguration().getCredentials()
        .setUsers(new StackGresClusterUsersCredentials());
    cluster.getSpec().getConfiguration().getCredentials().getUsers()
        .setSuperuser(new StackGresClusterUserSecretKeyRef());
    cluster.getSpec().getConfiguration().getCredentials().getUsers()
        .getSuperuser().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.SUPERUSER_USERNAME_ENV, "test-secret"));
    cluster.getSpec().getConfiguration().getCredentials().getUsers()
        .getSuperuser().setPassword(new SecretKeySelector(
            StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV, "test-secret"));
    cluster.getSpec().getConfiguration().getCredentials().getUsers()
        .setReplication(new StackGresClusterUserSecretKeyRef());
    cluster.getSpec().getConfiguration().getCredentials().getUsers()
        .getReplication().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.REPLICATION_USERNAME_ENV, "test-secret"));
    cluster.getSpec().getConfiguration().getCredentials().getUsers()
        .getReplication().setPassword(new SecretKeySelector(
            StackGresPasswordKeys.REPLICATION_PASSWORD_ENV, "test-secret"));
    cluster.getSpec().getConfiguration().getCredentials().getUsers()
        .setAuthenticator(new StackGresClusterUserSecretKeyRef());
    cluster.getSpec().getConfiguration().getCredentials().getUsers()
        .getAuthenticator().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV, "test-secret"));
    cluster.getSpec().getConfiguration().getCredentials().getUsers()
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
    cluster.getSpec().getConfiguration().setCredentials(new StackGresClusterCredentials());
    cluster.getSpec().getConfiguration().getCredentials()
        .setUsers(new StackGresClusterUsersCredentials());
    cluster.getSpec().getConfiguration().getCredentials().getUsers()
        .setSuperuser(new StackGresClusterUserSecretKeyRef());
    cluster.getSpec().getConfiguration().getCredentials().getUsers()
        .getSuperuser().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.SUPERUSER_USERNAME_ENV,
            key.equals(StackGresPasswordKeys.SUPERUSER_USERNAME_ENV)
            ? "missing-test-secret" : "test-secret"));
    cluster.getSpec().getConfiguration().getCredentials().getUsers()
        .getSuperuser().setPassword(new SecretKeySelector(
            StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV,
            key.equals(StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV)
            ? "missing-test-secret" : "test-secret"));
    cluster.getSpec().getConfiguration().getCredentials().getUsers()
        .setReplication(new StackGresClusterUserSecretKeyRef());
    cluster.getSpec().getConfiguration().getCredentials().getUsers()
        .getReplication().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.REPLICATION_USERNAME_ENV,
            key.equals(StackGresPasswordKeys.REPLICATION_USERNAME_ENV)
            ? "missing-test-secret" : "test-secret"));
    cluster.getSpec().getConfiguration().getCredentials().getUsers()
        .getReplication().setPassword(new SecretKeySelector(
            StackGresPasswordKeys.REPLICATION_PASSWORD_ENV,
            key.equals(StackGresPasswordKeys.REPLICATION_PASSWORD_ENV)
            ? "missing-test-secret" : "test-secret"));
    cluster.getSpec().getConfiguration().getCredentials().getUsers()
        .setAuthenticator(new StackGresClusterUserSecretKeyRef());
    cluster.getSpec().getConfiguration().getCredentials().getUsers()
        .getAuthenticator().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV,
            key.equals(StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV)
            ? "missing-test-secret" : "test-secret"));
    cluster.getSpec().getConfiguration().getCredentials().getUsers()
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

  private void mockPatroniCredentials() {
    cluster.getSpec().getConfiguration().setCredentials(new StackGresClusterCredentials());
    cluster.getSpec().getConfiguration().getCredentials()
        .setPatroni(new StackGresClusterPatroniCredentials());
    cluster.getSpec().getConfiguration().getCredentials().getPatroni()
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
    cluster.getSpec().getConfiguration().setCredentials(new StackGresClusterCredentials());
    cluster.getSpec().getConfiguration().getCredentials()
        .setPatroni(new StackGresClusterPatroniCredentials());
    cluster.getSpec().getConfiguration().getCredentials().getPatroni()
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
    cluster.getSpec().getConfiguration().setCredentials(new StackGresClusterCredentials());
    cluster.getSpec().getConfiguration().getCredentials()
        .setPatroni(new StackGresClusterPatroniCredentials());
    cluster.getSpec().getConfiguration().getCredentials().getPatroni()
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

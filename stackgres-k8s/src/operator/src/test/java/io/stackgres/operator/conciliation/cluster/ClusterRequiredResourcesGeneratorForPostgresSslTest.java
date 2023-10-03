/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresClusterSsl;
import io.stackgres.operator.conciliation.factory.cluster.PostgresSslSecret;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ClusterRequiredResourcesGeneratorForPostgresSslTest
    extends AbstractClusterRequiredResourcesGeneratorTest {

  @Test
  void givenClusterWithPostgresSsl_shouldIgnoreMissingSecret() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockPostgresSsl();
    unmockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfigurations().getSgPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getSgInstanceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));

    generator.getRequiredResources(cluster);

    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getSgInstanceProfile(), clusterNamespace);
    verify(secretFinder, times(3)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithPostgresSslWithPreviousSecret_shouldLookItUp() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockPostgresSslWithPreviousGeneratedSecret();
    unmockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfigurations().getSgPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getSgInstanceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));

    generator.getRequiredResources(cluster);

    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getSgInstanceProfile(), clusterNamespace);
    verify(secretFinder, times(3)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithPostgresSslWithSecret_shouldLookItUp() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockPostgresSslWithSecret();
    unmockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfigurations().getSgPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getSgInstanceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));

    generator.getRequiredResources(cluster);

    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getSgInstanceProfile(), clusterNamespace);
    verify(secretFinder, times(3)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithPostgresSslWithoutSecretForCertificate_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockPostgresSslWithMissingSecret("certificate");
    unmockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfigurations().getSgPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getSgInstanceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));

    assertException("Certificate secret missing-test-secret was not found");

    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getSgInstanceProfile(), clusterNamespace);
    verify(secretFinder, times(2)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithPostgresSslWithoutSecretForPrivateKey_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockPostgresSslWithMissingSecret("private-key");
    unmockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfigurations().getSgPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getSgInstanceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));

    assertException("Private key secret missing-test-secret was not found");

    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getSgInstanceProfile(), clusterNamespace);
    verify(secretFinder, times(3)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithPostgresSslWithoutKeyForCertificate_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockPostgresSslWithMissingKey("certificate");
    unmockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfigurations().getSgPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getSgInstanceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));

    assertException("Certificate key test-certificate was not found in secret test-secret");

    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getSgInstanceProfile(), clusterNamespace);
    verify(secretFinder, times(2)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithPostgresSslWithoutKeyForPrivateKey_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockPostgresSslWithMissingKey("private-key");
    unmockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfigurations().getSgPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getSgInstanceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));

    assertException("Private key key test-private-key was not found in secret test-secret");

    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getSgInstanceProfile(), clusterNamespace);
    verify(secretFinder, times(3)).findByNameAndNamespace(any(), any());
  }

  private void mockPostgresSsl() {
    cluster.getSpec().getPostgres().setSsl(new StackGresClusterSsl());
    cluster.getSpec().getPostgres().getSsl().setEnabled(true);
  }

  private void mockPostgresSslWithPreviousGeneratedSecret() {
    cluster.getSpec().getPostgres().setSsl(new StackGresClusterSsl());
    cluster.getSpec().getPostgres().getSsl().setEnabled(true);
    when(secretFinder.findByNameAndNamespace(
        PostgresSslSecret.name(cluster),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(new SecretBuilder()
            .withData(Map.ofEntries(
                Map.entry(
                    PostgresSslSecret.CERTIFICATE_KEY,
                    ResourceUtil.encodeSecret("test-certificate")),
                Map.entry(
                    PostgresSslSecret.PRIVATE_KEY_KEY,
                    ResourceUtil.encodeSecret("test-private-key"))))
            .build()));
  }

  private void mockPostgresSslWithSecret() {
    cluster.getSpec().getPostgres().setSsl(new StackGresClusterSsl());
    cluster.getSpec().getPostgres().getSsl().setEnabled(true);
    cluster.getSpec().getPostgres().getSsl()
        .setCertificateSecretKeySelector(new SecretKeySelector(
            "test-certificate",
            "test-secret"));
    cluster.getSpec().getPostgres().getSsl()
        .setPrivateKeySecretKeySelector(new SecretKeySelector(
            "test-private-key",
            "test-secret"));
    when(secretFinder.findByNameAndNamespace(
        "test-secret",
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(new SecretBuilder()
            .withData(Map.ofEntries(
                Map.entry(
                    "test-certificate",
                    ResourceUtil.encodeSecret("test-certificate")),
                Map.entry(
                    "test-private-key",
                    ResourceUtil.encodeSecret("test-private-key"))))
            .build()));
  }

  private void mockPostgresSslWithMissingSecret(String key) {
    cluster.getSpec().getPostgres().setSsl(new StackGresClusterSsl());
    cluster.getSpec().getPostgres().getSsl().setEnabled(true);
    cluster.getSpec().getPostgres().getSsl()
        .setCertificateSecretKeySelector(new SecretKeySelector(
            "test-certificate",
            key.equals("certificate")
            ? "missing-test-secret" : "test-secret"));
    cluster.getSpec().getPostgres().getSsl()
        .setPrivateKeySecretKeySelector(new SecretKeySelector(
            "test-private-key",
            key.equals("private-key")
            ? "missing-test-secret" : "test-secret"));
    when(secretFinder.findByNameAndNamespace(
        "test-secret",
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(new SecretBuilder()
            .withData(Map.ofEntries(
                Map.entry(
                    "test-certificate",
                    ResourceUtil.encodeSecret("test-certificate")),
                Map.entry(
                    "test-private-key",
                    ResourceUtil.encodeSecret("test-private-key"))))
            .build()));
  }

  private void mockPostgresSslWithMissingKey(String key) {
    cluster.getSpec().getPostgres().setSsl(new StackGresClusterSsl());
    cluster.getSpec().getPostgres().getSsl().setEnabled(true);
    cluster.getSpec().getPostgres().getSsl()
        .setCertificateSecretKeySelector(new SecretKeySelector(
            "test-certificate",
            "test-secret"));
    cluster.getSpec().getPostgres().getSsl()
        .setPrivateKeySecretKeySelector(new SecretKeySelector(
            "test-private-key",
            "test-secret"));
    when(secretFinder.findByNameAndNamespace(
        "test-secret",
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(new SecretBuilder()
            .withData(Map.ofEntries(
                Map.entry(
                    key.equals("certificate")
                    ? "missing-key" : "test-certificate",
                    ResourceUtil.encodeSecret("test-certificate")),
                Map.entry(
                    key.equals("private-key")
                    ? "missing-key" : "test-private-key",
                    ResourceUtil.encodeSecret("test-private-key"))))
            .build()));
  }

}

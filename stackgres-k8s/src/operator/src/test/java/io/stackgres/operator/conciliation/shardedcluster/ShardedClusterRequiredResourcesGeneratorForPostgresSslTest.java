/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import static io.stackgres.common.StackGresShardedClusterUtil.postgresSslSecretName;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresClusterSsl;
import io.stackgres.operator.conciliation.factory.shardedcluster.StackGresShardedClusterForCitusUtil;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ShardedClusterRequiredResourcesGeneratorForPostgresSslTest
    extends AbstractShardedClusterRequiredResourcesGeneratorTest {

  @Test
  void givenClusterWithPostgresSsl_shouldIgnoreMissingSecret() {
    mockPostgresSsl();
    mockPgConfig();

    generator.getRequiredResources(cluster);

    verify(postgresConfigFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(secretFinder, times(3)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithPostgresSslWithPreviousSecret_shouldLookItUp() {
    mockPostgresSslWithPreviousGeneratedSecret();
    mockPgConfig();

    generator.getRequiredResources(cluster);

    verify(postgresConfigFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(secretFinder, times(3)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithPostgresSslWithSecret_shouldLookItUp() {
    mockPostgresSslWithSecret();
    mockPgConfig();

    generator.getRequiredResources(cluster);

    verify(postgresConfigFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(secretFinder, times(3)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithPostgresSslWithoutSecretForCertificate_shouldFail() {
    mockPostgresSslWithMissingSecret("certificate");
    mockPgConfig();

    assertException("Certificate secret missing-test-secret was not found");

    verify(postgresConfigFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(secretFinder, times(2)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithPostgresSslWithoutSecretForPrivateKey_shouldFail() {
    mockPostgresSslWithMissingSecret("private-key");
    mockPgConfig();

    assertException("Private key secret missing-test-secret was not found");

    verify(postgresConfigFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(secretFinder, times(3)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithPostgresSslWithoutKeyForCertificate_shouldFail() {
    mockPostgresSslWithMissingKey("certificate");
    mockPgConfig();

    assertException("Certificate key test-certificate was not found in secret test-secret");

    verify(postgresConfigFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(secretFinder, times(2)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithPostgresSslWithoutKeyForPrivateKey_shouldFail() {
    mockPostgresSslWithMissingKey("private-key");
    mockPgConfig();

    assertException("Private key key test-private-key was not found in secret test-secret");

    verify(postgresConfigFinder, times(1)).findByNameAndNamespace(any(), any());
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
        postgresSslSecretName(cluster),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(new SecretBuilder()
            .withData(Map.ofEntries(
                Map.entry(
                    StackGresShardedClusterForCitusUtil.CERTIFICATE_KEY,
                    ResourceUtil.encodeSecret("test-certificate")),
                Map.entry(
                    StackGresShardedClusterForCitusUtil.PRIVATE_KEY_KEY,
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

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresClusterSsl;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedClusterPostgresSslContextAppenderTest {

  private ShardedClusterPostgresSslContextAppender contextAppender;

  private StackGresShardedCluster cluster;

  @Spy
  private StackGresShardedClusterContext.Builder contextBuilder;

  @Mock
  private ResourceFinder<Secret> secretFinder;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.shardedCluster().loadDefault().get();
    contextAppender = new ShardedClusterPostgresSslContextAppender(secretFinder);
  }

  @Test
  void givenClusterWithPostgresSsl_shouldIgnoreMissingSecret() {
    mockPostgresSsl();

    contextAppender.appendContext(cluster, contextBuilder);

    verify(secretFinder, times(2)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithPostgresSslWithPreviousSecret_shouldLookItUp() {
    mockPostgresSslWithPreviousGeneratedSecret();

    contextAppender.appendContext(cluster, contextBuilder);

    verify(secretFinder, times(2)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithPostgresSslWithSecret_shouldLookItUp() {
    mockPostgresSslWithSecret();

    contextAppender.appendContext(cluster, contextBuilder);

    verify(secretFinder, times(2)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithPostgresSslWithoutSecretForCertificate_shouldFail() {
    mockPostgresSslWithMissingSecret("certificate");

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Certificate secret missing-test-secret was not found", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(2)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithPostgresSslWithoutSecretForPrivateKey_shouldFail() {
    mockPostgresSslWithMissingSecret("private-key");

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Private key secret missing-test-secret was not found", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(2)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithPostgresSslWithoutKeyForCertificate_shouldFail() {
    mockPostgresSslWithMissingKey("certificate");

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Certificate key test-certificate was not found in secret test-secret", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(2)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithPostgresSslWithoutKeyForPrivateKey_shouldFail() {
    mockPostgresSslWithMissingKey("private-key");

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Private key key test-private-key was not found in secret test-secret", ex.getMessage());

    verify(secretFinder, Mockito.atLeastOnce()).findByNameAndNamespace(any(), any());
    verify(secretFinder, Mockito.atMost(2)).findByNameAndNamespace(any(), any());
  }

  private void mockPostgresSsl() {
    cluster.getSpec().getPostgres().setSsl(new StackGresClusterSsl());
    cluster.getSpec().getPostgres().getSsl().setEnabled(true);
  }

  private void mockPostgresSslWithPreviousGeneratedSecret() {
    cluster.getSpec().getPostgres().setSsl(new StackGresClusterSsl());
    cluster.getSpec().getPostgres().getSsl().setEnabled(true);
    when(secretFinder.findByNameAndNamespace(
        StackGresShardedClusterUtil.postgresSslSecretName(cluster),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(new SecretBuilder()
            .withData(Map.ofEntries(
                Map.entry(
                    PatroniUtil.CERTIFICATE_KEY,
                    ResourceUtil.encodeSecret("test-certificate")),
                Map.entry(
                    PatroniUtil.PRIVATE_KEY_KEY,
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

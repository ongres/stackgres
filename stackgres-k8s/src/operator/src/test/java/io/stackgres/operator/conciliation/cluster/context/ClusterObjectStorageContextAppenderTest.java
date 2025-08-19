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

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorageBuilder;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.cluster.backup.BackupEnvVarFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterObjectStorageContextAppenderTest {

  private ClusterObjectStorageContextAppender contextAppender;

  private StackGresCluster cluster;

  @Spy
  private StackGresClusterContext.Builder contextBuilder;

  @Mock
  private CustomResourceFinder<StackGresObjectStorage> objectStorageFinder;
  @Mock
  private ResourceFinder<Secret> secretFinder;
  @Mock
  private ClusterReplicationInitializationContextAppender clusterReplicationInitializationContextAppender;

  private BackupEnvVarFactory backupEnvVarFactory = new BackupEnvVarFactory();

  private StackGresObjectStorage objectStorage;
  private Secret secret;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    contextAppender = new ClusterObjectStorageContextAppender(
        objectStorageFinder,
        secretFinder,
        backupEnvVarFactory,
        clusterReplicationInitializationContextAppender);
    objectStorage = new StackGresObjectStorageBuilder()
        .withNewMetadata()
        .withName("objectstorage")
        .endMetadata()
        .withNewSpec()
        .withType("s3Compatible")
        .withNewS3Compatible()
        .withBucket("test")
        .withNewAwsCredentials()
        .withNewSecretKeySelectors()
        .withNewAccessKeyId()
        .withName("test")
        .withKey("accessKeyId")
        .endAccessKeyId()
        .withNewSecretAccessKey()
        .withName("test")
        .withKey("secretAccessKey")
        .endSecretAccessKey()
        .endSecretKeySelectors()
        .endAwsCredentials()
        .endS3Compatible()
        .endSpec()
        .build();
    secret = new SecretBuilder()
        .withData(Map.of(
            "accessKeyId", "test",
            "secretAccessKey", "test"))
        .build();
  }

  @Test
  void givenClusterWithObjectStorage_shouldPass() {
    when(objectStorageFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(objectStorage));
    when(secretFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(secret));

    contextAppender.appendContext(cluster, contextBuilder, cluster.getSpec().getPostgres().getVersion());

    verify(objectStorageFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(secretFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterReplicationInitializationContextAppender, times(1)).appendContext(any(), any(), any(), any());
  }

  @Test
  void givenClusterWithMissingObjectStorage_shouldFail() {
    cluster.getSpec().getConfigurations().getBackups().get(0).setSgObjectStorage("missing-object-storage");

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(
            cluster, contextBuilder, cluster.getSpec().getPostgres().getVersion()));
    assertEquals("SGObjectStorage missing-object-storage not found", ex.getMessage());

    verify(objectStorageFinder, times(1)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithObjectStorageWithMissingSecret_shouldFail() {
    when(objectStorageFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(objectStorage));

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(
            cluster, contextBuilder, cluster.getSpec().getPostgres().getVersion()));
    assertEquals("Secret test not found for SGObjectStorage objectstorage", ex.getMessage());

    verify(objectStorageFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(secretFinder, times(1)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithObjectStorageWithMissingSecretKey_shouldFail() {
    when(objectStorageFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(objectStorage));
    when(secretFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(new Secret()));

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(
            cluster, contextBuilder, cluster.getSpec().getPostgres().getVersion()));
    assertEquals("Key accessKeyId not found in Secret test for SGObjectStorage objectstorage", ex.getMessage());

    verify(objectStorageFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(secretFinder, times(1)).findByNameAndNamespace(any(), any());
  }

}

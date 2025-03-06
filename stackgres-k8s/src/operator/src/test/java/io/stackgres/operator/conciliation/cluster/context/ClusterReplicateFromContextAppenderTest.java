/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromBuilder;
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
class ClusterReplicateFromContextAppenderTest {

  private ClusterReplicateFromContextAppender contextAppender;

  private StackGresCluster cluster;

  @Spy
  private StackGresClusterContext.Builder contextBuilder;

  @Mock
  private CustomResourceFinder<StackGresObjectStorage> objectStorageFinder;

  @Mock
  private ResourceFinder<Secret> secretFinder;

  @Mock
  private CustomResourceFinder<StackGresCluster> clusterFinder;

  private BackupEnvVarFactory backupEnvVarFactory = new BackupEnvVarFactory();

  private StackGresObjectStorage objectStorage;
  private Secret secret;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    contextAppender = new ClusterReplicateFromContextAppender(
        objectStorageFinder, secretFinder, backupEnvVarFactory, clusterFinder);
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
  void givenClusterWithoutReplicateFrom_shouldPass() {
    contextAppender.appendContext(cluster, contextBuilder);
    verify(contextBuilder).replicateCluster(Optional.empty());
    verify(contextBuilder).replicateObjectStorageConfig(Optional.empty());
    verify(contextBuilder).replicateSecrets(Map.of());
  }

  @Test
  void givenClusterWithReplicateFromClusterWithoutBackups_shouldPass() {
    cluster.getSpec().setReplicateFrom(
        new StackGresClusterReplicateFromBuilder()
        .withNewInstance()
        .withSgCluster("other-cluster")
        .endInstance()
        .build());
    final StackGresCluster sourceCluster =
        new StackGresClusterBuilder()
        .withNewMetadata()
        .withName("other-cluster")
        .endMetadata()
        .build();
    when(clusterFinder.findByNameAndNamespace("other-cluster", cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(sourceCluster));

    contextAppender.appendContext(cluster, contextBuilder);

    verify(contextBuilder).replicateCluster(Optional.of(sourceCluster));
    verify(contextBuilder).replicateObjectStorageConfig(Optional.empty());
    verify(contextBuilder).replicateSecrets(Map.of());
  }

  @Test
  void givenClusterWithReplicateFromClusterWithBackups_shouldPass() {
    cluster.getSpec().setReplicateFrom(
        new StackGresClusterReplicateFromBuilder()
        .withNewInstance()
        .withSgCluster("other-cluster")
        .endInstance()
        .build());
    final StackGresCluster sourceCluster =
        new StackGresClusterBuilder()
        .withNewMetadata()
        .withName("other-cluster")
        .endMetadata()
        .withNewSpec()
        .withNewConfigurations()
        .addNewBackup()
        .withPath("test")
        .withSgObjectStorage("objectstorage")
        .endBackup()
        .endConfigurations()
        .endSpec()
        .build();
    when(clusterFinder.findByNameAndNamespace("other-cluster", cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(sourceCluster));
    when(secretFinder.findByNameAndNamespace("test", cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(secret));
    when(objectStorageFinder.findByNameAndNamespace("objectstorage", cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(objectStorage));

    contextAppender.appendContext(cluster, contextBuilder);

    verify(contextBuilder).replicateCluster(Optional.of(sourceCluster));
    verify(contextBuilder).replicateObjectStorageConfig(Optional.of(objectStorage));
    verify(contextBuilder).replicateSecrets(Map.of("test", secret));
  }

}

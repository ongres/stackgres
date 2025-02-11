/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext.Builder;
import io.stackgres.operator.conciliation.factory.cluster.backup.BackupEnvVarFactory;
import jakarta.enterprise.context.ApplicationScoped;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class ClusterObjectStorageContextAppender
    extends ContextAppender<StackGresCluster, StackGresClusterContext.Builder> {

  private final CustomResourceFinder<StackGresObjectStorage> objectStorageFinder;
  private final ResourceFinder<Secret> secretFinder;
  private final BackupEnvVarFactory backupEnvVarFactory;
  private final ClusterReplicationInitializationContextAppender clusterReplicationInitializationContextAppender;

  public ClusterObjectStorageContextAppender(
      CustomResourceFinder<StackGresObjectStorage> objectStorageFinder,
      ResourceFinder<Secret> secretFinder,
      BackupEnvVarFactory backupEnvVarFactory,
      ClusterReplicationInitializationContextAppender clusterReplicationInitializationContextAppender) {
    this.objectStorageFinder = objectStorageFinder;
    this.secretFinder = secretFinder;
    this.backupEnvVarFactory = backupEnvVarFactory;
    this.clusterReplicationInitializationContextAppender = clusterReplicationInitializationContextAppender;
  }

  @Override
  public void appendContext(StackGresCluster cluster, Builder contextBuilder) {
    final Optional<StackGresObjectStorage> backupObjectStorage = Optional
        .ofNullable(cluster.getSpec().getConfigurations().getBackups())
        .map(Collection::stream)
        .flatMap(Stream::findFirst)
        .map(StackGresClusterBackupConfiguration::getSgObjectStorage)
        .map(backupObjectStorageName -> objectStorageFinder
            .findByNameAndNamespace(
                backupObjectStorageName,
                cluster.getMetadata().getNamespace())
            .orElseThrow(() -> new IllegalArgumentException(
                "SGObjectStorage " + backupObjectStorageName + " not found")));
    final Map<String, Secret> backupSecrets = backupObjectStorage
        .map(StackGresObjectStorage::getSpec)
        .stream()
        .flatMap(backupEnvVarFactory::streamStorageSecretReferences)
        .collect(Collectors.groupingBy(
            Function.<SecretKeySelector>identity()
            .andThen(SecretKeySelector::getName)))
        .entrySet()
        .stream()
        .map(entry -> Tuple.tuple(
            entry.getKey(),
            secretFinder
            .findByNameAndNamespace(
                entry.getKey(),
                cluster.getMetadata().getNamespace())
            .orElseThrow(() -> new IllegalArgumentException(
                "Secret " + entry.getKey() + " not found"
                    + " for SGObjectStorage "
                    + backupObjectStorage.get().getMetadata().getName())),
            entry))
        .map(t -> {
          t.v3.getValue().stream()
              .map(SecretKeySelector::getKey)
              .forEach(key -> Optional.of(t.v2)
                  .map(Secret::getData)
                  .map(data -> data.get(key))
                  .orElseThrow(() -> new IllegalArgumentException(
                      "Key " + key + " not found in Secret " + t.v1()
                      + " for SGObjectStorage "
                      + backupObjectStorage.get().getMetadata().getName())));
          return t.limit2();
        })
        .collect(Collectors.toMap(Tuple2::v1, Tuple2::v2));
    
    contextBuilder
        .objectStorage(backupObjectStorage)
        .backupSecrets(backupSecrets);

    clusterReplicationInitializationContextAppender
        .appendContext(cluster, backupObjectStorage, contextBuilder);
  }

}

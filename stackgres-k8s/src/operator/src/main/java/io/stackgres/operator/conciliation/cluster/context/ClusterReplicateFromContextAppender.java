/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFrom;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromInstance;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromStorage;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
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
public class ClusterReplicateFromContextAppender
    extends ContextAppender<StackGresCluster, StackGresClusterContext.Builder> {

  private final CustomResourceFinder<StackGresObjectStorage> objectStorageFinder;
  private final ResourceFinder<Secret> secretFinder;
  private final BackupEnvVarFactory backupEnvVarFactory;
  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  public ClusterReplicateFromContextAppender(
      CustomResourceFinder<StackGresObjectStorage> objectStorageFinder,
      ResourceFinder<Secret> secretFinder,
      BackupEnvVarFactory backupEnvVarFactory,
      CustomResourceFinder<StackGresCluster> clusterFinder) {
    this.objectStorageFinder = objectStorageFinder;
    this.secretFinder = secretFinder;
    this.backupEnvVarFactory = backupEnvVarFactory;
    this.clusterFinder = clusterFinder;
  }

  @Override
  public void appendContext(StackGresCluster cluster, Builder contextBuilder) {
    final Optional<StackGresCluster> replicateCluster =
        Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getReplicateFrom)
        .map(StackGresClusterReplicateFrom::getInstance)
        .map(StackGresClusterReplicateFromInstance::getSgCluster)
        .flatMap(sgCluster -> Optional.of(
            clusterFinder.findByNameAndNamespace(
                sgCluster,
                cluster.getMetadata().getNamespace())
            .orElseThrow(() -> new IllegalArgumentException("Can not find SGCluster "
                + sgCluster + " to replicate from"))));
    final Optional<StackGresObjectStorage> replicateObjectStorage =
        replicateCluster
        .flatMap(replicateFromCluster -> Optional.of(replicateFromCluster)
            .map(StackGresCluster::getSpec)
            .map(StackGresClusterSpec::getConfigurations)
            .map(StackGresClusterConfigurations::getBackups)
            .stream()
            .flatMap(List::stream)
            .findFirst()
            .map(StackGresClusterBackupConfiguration::getSgObjectStorage))
        .or(() -> Optional.of(cluster)
            .map(StackGresCluster::getSpec)
            .map(StackGresClusterSpec::getReplicateFrom)
            .map(StackGresClusterReplicateFrom::getStorage)
            .map(StackGresClusterReplicateFromStorage::getSgObjectStorage))
        .map(sgObjectStorage -> objectStorageFinder
            .findByNameAndNamespace(
                sgObjectStorage,
                cluster.getMetadata().getNamespace())
            .orElseThrow(() -> new IllegalArgumentException("Can not find SGObjectStorage "
                + sgObjectStorage + " to replicate from")));

    final Map<String, Secret> replicateSecrets = replicateObjectStorage
        .map(StackGresObjectStorage::getSpec)
        .stream()
        .flatMap(backupEnvVarFactory::streamStorageSecretReferences)
        .map(secretKeySelector -> secretKeySelector.getName())
        .collect(Collectors.groupingBy(Function.identity()))
        .keySet()
        .stream()
        .map(name -> Tuple.tuple(
            name,
            secretFinder
            .findByNameAndNamespace(
                name,
                cluster.getMetadata().getNamespace())
            .orElseThrow(() -> new IllegalArgumentException(
                "Secret " + name + " not found"))))
        .collect(Collectors.toMap(Tuple2::v1, Tuple2::v2));

    contextBuilder
        .replicateCluster(replicateCluster)
        .replicateObjectStorageConfig(replicateObjectStorage)
        .replicateSecrets(replicateSecrets);
  }

}

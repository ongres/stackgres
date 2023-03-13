/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import static io.stackgres.common.StackGresShardedClusterUtil.getCoordinatorCluster;
import static io.stackgres.common.StackGresShardedClusterUtil.getShardsCluster;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.VersionInfo;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinator;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShards;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.RequiredResourceDecorator;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.cluster.ImmutableStackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ShardedClusterRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresShardedCluster> {

  protected static final Logger LOGGER = LoggerFactory
      .getLogger(ShardedClusterRequiredResourcesGenerator.class);

  private final Supplier<VersionInfo> kubernetesVersionSupplier;

  private final CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;

  private final CustomResourceFinder<StackGresPoolingConfig> poolingConfigFinder;

  private final CustomResourceFinder<StackGresProfile> profileFinder;

  private final ResourceFinder<Secret> secretFinder;

  private final ResourceFinder<Service> serviceFinder;

  private final RequiredResourceDecorator<StackGresShardedClusterContext> decorator;

  @Inject
  public ShardedClusterRequiredResourcesGenerator(
      Supplier<VersionInfo> kubernetesVersionSupplier,
      CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder,
      CustomResourceFinder<StackGresPoolingConfig> poolingConfigFinder,
      CustomResourceFinder<StackGresProfile> profileFinder,
      ResourceFinder<Secret> secretFinder,
      ResourceFinder<Service> serviceFinder,
      RequiredResourceDecorator<StackGresShardedClusterContext> decorator) {
    this.kubernetesVersionSupplier = kubernetesVersionSupplier;
    this.postgresConfigFinder = postgresConfigFinder;
    this.poolingConfigFinder = poolingConfigFinder;
    this.profileFinder = profileFinder;
    this.secretFinder = secretFinder;
    this.serviceFinder = serviceFinder;
    this.decorator = decorator;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresShardedCluster config) {
    final ObjectMeta metadata = config.getMetadata();
    final String clusterName = metadata.getName();
    final String clusterNamespace = metadata.getNamespace();

    VersionInfo kubernetesVersion = kubernetesVersionSupplier.get();

    Optional<Secret> databaseSecret = secretFinder
        .findByNameAndNamespace(clusterName, clusterNamespace);

    StackGresClusterContext coordinatorContext = getCoordinatorContext(
        clusterName, clusterNamespace, config, kubernetesVersion, databaseSecret);

    List<StackGresClusterContext> shardsContexts = getShardsContexts(
        clusterName, clusterNamespace, config, kubernetesVersion, databaseSecret);

    Optional<Service> coordinatorPrimaryService = serviceFinder
        .findByNameAndNamespace(
            PatroniUtil.readWriteName(coordinatorContext.getCluster()), clusterNamespace);
    StackGresShardedClusterContext context = ImmutableStackGresShardedClusterContext.builder()
        .kubernetesVersion(kubernetesVersion)
        .source(config)
        .databaseSecret(databaseSecret)
        .coordinator(coordinatorContext)
        .shards(shardsContexts)
        .coordinatorPrimaryService(coordinatorPrimaryService)
        .build();

    return decorator.decorateResources(context);
  }

  private StackGresClusterContext getCoordinatorContext(
      final String clusterName,
      final String clusterNamespace,
      StackGresShardedCluster config,
      VersionInfo kubernetesVersion,
      Optional<Secret> databaseSecret) {
    final StackGresShardedClusterSpec spec = config.getSpec();
    final StackGresShardedClusterCoordinator coordinator = spec.getCoordinator();
    final StackGresClusterConfiguration configuration = coordinator.getConfiguration();
    final StackGresPostgresConfig pgConfig = postgresConfigFinder
        .findByNameAndNamespace(configuration.getPostgresConfig(), clusterNamespace)
        .orElseThrow(() -> new IllegalArgumentException(
            "Coordinator of SGShardedCluster " + clusterNamespace + "." + clusterName
                + " have a non existent " + StackGresPostgresConfig.KIND
                + " " + configuration.getPostgresConfig()));

    final StackGresProfile profile = profileFinder
        .findByNameAndNamespace(coordinator.getResourceProfile(), clusterNamespace)
        .orElseThrow(() -> new IllegalArgumentException(
            "Coordinator of SGShardedCluster " + clusterNamespace + "." + clusterName
                + " have a non existent " + StackGresProfile.KIND
                + " " + coordinator.getResourceProfile()));

    final Optional<StackGresPoolingConfig> pooling = Optional
        .ofNullable(configuration.getConnectionPoolingConfig())
        .flatMap(poolingConfigName -> poolingConfigFinder
            .findByNameAndNamespace(poolingConfigName, clusterNamespace));

    StackGresCluster coordinatorCluster = getCoordinatorCluster(config);

    StackGresClusterContext coordinatorContext = ImmutableStackGresClusterContext.builder()
        .kubernetesVersion(kubernetesVersion)
        .source(coordinatorCluster)
        .postgresConfig(pgConfig)
        .profile(profile)
        .poolingConfig(pooling)
        .databaseSecret(databaseSecret)
        .build();
    return coordinatorContext;
  }

  private List<StackGresClusterContext> getShardsContexts(
      final String clusterName,
      final String clusterNamespace,
      StackGresShardedCluster config,
      VersionInfo kubernetesVersion,
      Optional<Secret> databaseSecret) {
    final StackGresShardedClusterSpec spec = config.getSpec();
    final StackGresShardedClusterShards shards = spec.getShards();
    final StackGresClusterConfiguration configuration = shards.getConfiguration();
    final StackGresPostgresConfig pgConfig = postgresConfigFinder
        .findByNameAndNamespace(configuration.getPostgresConfig(), clusterNamespace)
        .orElseThrow(() -> new IllegalArgumentException(
            "Shards of SGShardedCluster " + clusterNamespace + "." + clusterName
                + " have a non existent " + StackGresPostgresConfig.KIND
                + " " + configuration.getPostgresConfig()));

    final StackGresProfile profile = profileFinder
        .findByNameAndNamespace(shards.getResourceProfile(), clusterNamespace)
        .orElseThrow(() -> new IllegalArgumentException(
            "Shards of SGShardedCluster " + clusterNamespace + "." + clusterName
                + " have a non existent " + StackGresProfile.KIND
                + " " + shards.getResourceProfile()));

    final Optional<StackGresPoolingConfig> pooling = Optional
        .ofNullable(configuration.getConnectionPoolingConfig())
        .flatMap(poolingConfigName -> poolingConfigFinder
            .findByNameAndNamespace(poolingConfigName, clusterNamespace));

    return IntStream.range(0, config.getSpec().getShards().getClusters())
        .mapToObj(index -> getShardContext(
            config, kubernetesVersion, databaseSecret,
            pgConfig, profile, pooling, index))
        .toList();
  }

  private StackGresClusterContext getShardContext(
      StackGresShardedCluster config,
      VersionInfo kubernetesVersion,
      Optional<Secret> databaseSecret,
      StackGresPostgresConfig pgConfig,
      StackGresProfile profile,
      Optional<StackGresPoolingConfig> pooling,
      int index) {
    StackGresCluster shardsCluster = getShardsCluster(config, index);

    StackGresClusterContext shardsContext = ImmutableStackGresClusterContext.builder()
        .kubernetesVersion(kubernetesVersion)
        .source(shardsCluster)
        .postgresConfig(pgConfig)
        .profile(profile)
        .poolingConfig(pooling)
        .databaseSecret(databaseSecret)
        .build();
    return shardsContext;
  }

}

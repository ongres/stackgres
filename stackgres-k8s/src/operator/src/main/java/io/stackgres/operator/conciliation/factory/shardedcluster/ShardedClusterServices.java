/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresPort;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.postgres.service.StackGresPostgresServiceNodePort;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterPostgresCoordinatorServices;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterPostgresServices;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterPostgresShardsServices;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpecAnnotations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpecLabels;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpecMetadata;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardingType;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder
public class ShardedClusterServices implements
    ResourceGenerator<StackGresShardedClusterContext> {

  private final LabelFactoryForShardedCluster labelFactory;
  private final LabelFactoryForCluster clusterLabelFactory;

  @Inject
  public ShardedClusterServices(
      LabelFactoryForShardedCluster labelFactory,
      LabelFactoryForCluster clusterLabelFactory) {
    this.labelFactory = labelFactory;
    this.clusterLabelFactory = clusterLabelFactory;
  }

  /**
   * Create the Services associated with the cluster.
   */
  @Override
  public Stream<HasMetadata> generateResource(StackGresShardedClusterContext context) {
    Seq<HasMetadata> services = Seq.of();

    var coordinatorServices = context.getSource().getSpec().getPostgresServices().getCoordinator();
    if (Optional.of(coordinatorServices.getAny())
        .map(StackGresPostgresService::getEnabled)
        .orElse(true)
        && !StackGresShardingType.SHARDING_SPHERE.equals(
            StackGresShardingType.fromString(context.getShardedCluster().getSpec().getType()))) {
      services = services.append(createCoordinatorAnyService(context));
    }

    if (Optional.of(coordinatorServices.getPrimary())
        .map(StackGresPostgresService::getEnabled)
        .orElse(true)) {
      services = services.append(createCoordinatorPrimaryService(context));
    }

    var shardsServices = context.getSource().getSpec().getPostgresServices().getShards();
    if (Optional.of(shardsServices.getPrimaries())
        .map(StackGresPostgresService::getEnabled)
        .orElse(true)) {
      services = services.append(createShardsAnyPrimaryService(context));
    }

    return services;
  }

  private Service createCoordinatorAnyService(StackGresShardedClusterContext context) {
    StackGresShardedCluster cluster = context.getSource();
    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(StackGresShardedClusterUtil.anyCoordinatorServiceName(
            context.getSource()))
        .addToAnnotations(
            Optional.ofNullable(cluster.getSpec().getMetadata())
            .map(StackGresShardedClusterSpecMetadata::getAnnotations)
            .map(StackGresShardedClusterSpecAnnotations::getCoordinatorAnyService)
            .orElse(Map.of()))
        .addToLabels(labelFactory.genericLabels(cluster))
        .addToLabels(
            Optional.ofNullable(cluster.getSpec().getMetadata())
            .map(StackGresShardedClusterSpecMetadata::getLabels)
            .map(StackGresShardedClusterSpecLabels::getServices)
            .orElse(Map.of()))
        .addToLabels(
            Optional.ofNullable(cluster.getSpec().getMetadata())
            .map(StackGresShardedClusterSpecMetadata::getLabels)
            .map(StackGresShardedClusterSpecLabels::getCoordinatorAnyService)
            .orElse(Map.of()))
        .endMetadata()
        .withSpec(cluster.getSpec().getPostgresServices().getCoordinator().getAny())
        .editSpec()
        .addAllToPorts(List.of(
            new ServicePortBuilder()
                .withNodePort(Optional
                        .ofNullable(cluster.getSpec().getPostgresServices().getCoordinator().getAny())
                        .map(StackGresPostgresService::getNodePorts)
                        .map(StackGresPostgresServiceNodePort::getPgport)
                        .orElse(null))
                .withProtocol("TCP")
                .withName(EnvoyUtil.POSTGRES_PORT_NAME)
                .withPort(PatroniUtil.POSTGRES_SERVICE_PORT)
                .withTargetPort(new IntOrString(EnvoyUtil.POSTGRES_PORT_NAME))
                .build(),
            new ServicePortBuilder()
                .withNodePort(Optional
                        .ofNullable(cluster.getSpec().getPostgresServices().getCoordinator().getAny())
                        .map(StackGresPostgresService::getNodePorts)
                        .map(StackGresPostgresServiceNodePort::getReplicationport)
                        .orElse(null))
                .withProtocol("TCP")
                .withName(EnvoyUtil.POSTGRES_REPLICATION_PORT_NAME)
                .withPort(PatroniUtil.REPLICATION_SERVICE_PORT)
                .withTargetPort(new IntOrString(EnvoyUtil.POSTGRES_REPLICATION_PORT_NAME))
                .build()))
        .addAllToPorts(
            Optional.of(context.getSource().getSpec().getPostgresServices())
            .map(StackGresShardedClusterPostgresServices::getCoordinator)
            .map(StackGresShardedClusterPostgresCoordinatorServices::getCustomPorts)
            .stream()
            .flatMap(List::stream)
            .map(ServicePortBuilder::new)
            .map(this::setCustomPort)
            .map(ServicePortBuilder::build)
            .toList())
        .withSelector(clusterLabelFactory.clusterLabelsWithoutUid(
            context.getCoordinator()))
        .endSpec()
        .build();
  }

  private Service createCoordinatorPrimaryService(StackGresShardedClusterContext context) {
    StackGresShardedCluster cluster = context.getSource();
    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(StackGresShardedClusterUtil.primaryCoordinatorServiceName(
            context.getSource()))
        .addToAnnotations(
            Optional.ofNullable(cluster.getSpec().getMetadata())
            .map(StackGresShardedClusterSpecMetadata::getAnnotations)
            .map(StackGresShardedClusterSpecAnnotations::getCoordinatorPrimaryService)
            .orElse(Map.of()))
        .addToLabels(labelFactory.genericLabels(cluster))
        .addToLabels(
            Optional.ofNullable(cluster.getSpec().getMetadata())
            .map(StackGresShardedClusterSpecMetadata::getLabels)
            .map(StackGresShardedClusterSpecLabels::getServices)
            .orElse(Map.of()))
        .addToLabels(
            Optional.ofNullable(cluster.getSpec().getMetadata())
            .map(StackGresShardedClusterSpecMetadata::getLabels)
            .map(StackGresShardedClusterSpecLabels::getCoordinatorPrimaryService)
            .orElse(Map.of()))
        .endMetadata()
        .withSpec(cluster.getSpec().getPostgresServices().getCoordinator().getPrimary())
        .editSpec()
        .addAllToPorts(List.of(
            new ServicePortBuilder()
                .withNodePort(Optional
                        .ofNullable(cluster.getSpec().getPostgresServices().getCoordinator().getPrimary())
                        .map(StackGresPostgresService::getNodePorts)
                        .map(StackGresPostgresServiceNodePort::getPgport)
                        .orElse(null))
                .withProtocol("TCP")
                .withName(EnvoyUtil.POSTGRES_PORT_NAME)
                .withPort(PatroniUtil.POSTGRES_SERVICE_PORT)
                .withTargetPort(new IntOrString(EnvoyUtil.POSTGRES_PORT_NAME))
                .build()))
        .addAllToPorts(Seq.of(
            new ServicePortBuilder()
                .withNodePort(Optional
                        .ofNullable(cluster.getSpec().getPostgresServices().getCoordinator().getPrimary())
                        .map(StackGresPostgresService::getNodePorts)
                        .map(StackGresPostgresServiceNodePort::getReplicationport)
                        .orElse(null))
                .withProtocol("TCP")
                .withName(EnvoyUtil.POSTGRES_REPLICATION_PORT_NAME)
                .withPort(PatroniUtil.REPLICATION_SERVICE_PORT)
                .withTargetPort(new IntOrString(EnvoyUtil.POSTGRES_REPLICATION_PORT_NAME))
                .build())
            .filter(ignore -> !StackGresShardingType.SHARDING_SPHERE.equals(
                StackGresShardingType.fromString(context.getShardedCluster().getSpec().getType())))
            .toList())
        .addAllToPorts(
            Optional.of(context.getSource().getSpec().getPostgresServices())
            .map(StackGresShardedClusterPostgresServices::getCoordinator)
            .map(StackGresShardedClusterPostgresCoordinatorServices::getCustomPorts)
            .stream()
            .flatMap(List::stream)
            .map(ServicePortBuilder::new)
            .map(this::setCustomPort)
            .map(ServicePortBuilder::build)
            .toList())
        .endSpec()
        .build();
  }

  private Service createShardsAnyPrimaryService(StackGresShardedClusterContext context) {
    StackGresShardedCluster cluster = context.getSource();
    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(StackGresShardedClusterUtil.primariesShardsServiceName(
            context.getSource()))
        .addToAnnotations(
            Optional.ofNullable(cluster.getSpec().getMetadata())
            .map(StackGresShardedClusterSpecMetadata::getAnnotations)
            .map(StackGresShardedClusterSpecAnnotations::getShardsPrimariesService)
            .orElse(Map.of()))
        .addToLabels(labelFactory.genericLabels(cluster))
        .addToLabels(
            Optional.ofNullable(cluster.getSpec().getMetadata())
            .map(StackGresShardedClusterSpecMetadata::getLabels)
            .map(StackGresShardedClusterSpecLabels::getServices)
            .orElse(Map.of()))
        .addToLabels(
            Optional.ofNullable(cluster.getSpec().getMetadata())
            .map(StackGresShardedClusterSpecMetadata::getLabels)
            .map(StackGresShardedClusterSpecLabels::getShardsPrimariesService)
            .orElse(Map.of()))
        .endMetadata()
        .withSpec(cluster.getSpec().getPostgresServices().getShards().getPrimaries())
        .editSpec()
        .addAllToPorts(List.of(
            new ServicePortBuilder()
                .withNodePort(Optional
                        .ofNullable(cluster.getSpec().getPostgresServices().getShards().getPrimaries())
                        .map(StackGresPostgresService::getNodePorts)
                        .map(StackGresPostgresServiceNodePort::getPgport)
                        .orElse(null))
                .withProtocol("TCP")
                .withName(EnvoyUtil.POSTGRES_PORT_NAME)
                .withPort(PatroniUtil.POSTGRES_SERVICE_PORT)
                .withTargetPort(new IntOrString(EnvoyUtil.POSTGRES_PORT_NAME))
                .build(),
            new ServicePortBuilder()
                .withNodePort(Optional
                        .ofNullable(cluster.getSpec().getPostgresServices().getShards().getPrimaries())
                        .map(StackGresPostgresService::getNodePorts)
                        .map(StackGresPostgresServiceNodePort::getReplicationport)
                        .orElse(null))
                .withProtocol("TCP")
                .withName(EnvoyUtil.POSTGRES_REPLICATION_PORT_NAME)
                .withPort(PatroniUtil.REPLICATION_SERVICE_PORT)
                .withTargetPort(new IntOrString(EnvoyUtil.POSTGRES_REPLICATION_PORT_NAME))
                .build()))
        .addAllToPorts(
            Optional.of(context.getSource().getSpec().getPostgresServices())
            .map(StackGresShardedClusterPostgresServices::getShards)
            .map(StackGresShardedClusterPostgresShardsServices::getCustomPorts)
            .stream()
            .flatMap(List::stream)
            .map(ServicePortBuilder::new)
            .map(this::setCustomPort)
            .map(ServicePortBuilder::build)
            .toList())
        .endSpec()
        .build();
  }

  private ServicePortBuilder setCustomPort(ServicePortBuilder builder) {
    builder.withName(StackGresPort.CUSTOM.getName(builder.getName()));
    var targetPort = builder.buildTargetPort();
    if (targetPort.getStrVal() != null) {
      return builder.withTargetPort(new IntOrString(
          StackGresPort.CUSTOM.getName(
              builder.buildTargetPort().getStrVal())));
    }
    if (builder.getProtocol() == null) {
      builder.withProtocol("TCP");
    }
    return builder;
  }

}

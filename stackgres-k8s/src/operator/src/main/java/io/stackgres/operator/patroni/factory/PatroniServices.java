/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni.factory;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresService;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresServiceType;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresServices;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.common.LabelFactoryDelegator;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterResourceStreamFactory;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class PatroniServices implements StackGresClusterResourceStreamFactory {

  private LabelFactoryDelegator factoryDelegator;

  public static String name(StackGresClusterContext clusterContext) {
    String name = clusterContext.getCluster().getMetadata().getName();
    return PatroniUtil.name(name);
  }

  public static String readWriteName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getCluster().getMetadata().getName();
    return PatroniUtil.readWriteName(name);
  }

  public static String readOnlyName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getCluster().getMetadata().getName();
    return PatroniUtil.readOnlyName(name);
  }

  public String failoverName(StackGresClusterContext clusterContext) {
    final StackGresCluster cluster = clusterContext.getCluster();
    final LabelFactory<?> labelFactory = factoryDelegator.pickFactory(clusterContext);
    final String scope = labelFactory.clusterScope(cluster);
    return ResourceUtil.resourceName(
        scope + PatroniUtil.FAILOVER_SERVICE);
  }

  public String configName(StackGresClusterContext clusterContext) {
    final StackGresCluster cluster = clusterContext.getCluster();
    final LabelFactory<?> labelFactory = factoryDelegator.pickFactory(clusterContext);
    final String scope = labelFactory.clusterScope(cluster);
    return ResourceUtil.resourceName(
        scope + PatroniUtil.CONFIG_SERVICE);
  }

  /**
   * Create the Services associated with the cluster.
   */
  @Override
  public Stream<HasMetadata> streamResources(StackGresGeneratorContext context) {
    final StackGresClusterContext clusterContext = context.getClusterContext();
    final StackGresCluster cluster = clusterContext.getCluster();

    Service config = createConfigService(context);

    Seq<HasMetadata> services = Seq.of(config);

    boolean isPrimaryServiceEnabled = Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPostgresServices)
        .map(StackGresClusterPostgresServices::getPrimary)
        .map(StackGresClusterPostgresService::getEnabled)
        .orElse(true);

    if (isPrimaryServiceEnabled) {
      Service patroni = createPatroniService(context);
      services = services.append(patroni);
      Service primary = createPrimaryService(context);
      services = services.append(primary);
    }

    boolean isReplicaServiceEnabled = Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPostgresServices)
        .map(StackGresClusterPostgresServices::getReplicas)
        .map(StackGresClusterPostgresService::getEnabled)
        .orElse(true);

    if (isReplicaServiceEnabled) {
      Service replicas = createReplicaService(context);
      services = services.append(replicas);
    }

    return services;
  }

  private Service createConfigService(StackGresGeneratorContext context) {
    final StackGresClusterContext clusterContext = context.getClusterContext();
    final StackGresCluster cluster = clusterContext.getCluster();

    final LabelFactory<?> labelFactory = factoryDelegator.pickFactory(clusterContext);

    final Map<String, String> clusterLabels = labelFactory.clusterLabels(cluster);

    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(configName(clusterContext))
        .withLabels(clusterLabels)
        .withOwnerReferences(context.getClusterContext().getOwnerReferences())
        .endMetadata()
        .withNewSpec()
        .withClusterIP("None")
        .endSpec()
        .build();
  }

  private Service createPatroniService(StackGresGeneratorContext context) {
    StackGresClusterContext clusterContext = context.getClusterContext();
    StackGresCluster cluster = clusterContext.getCluster();

    final LabelFactory<?> labelFactory = factoryDelegator.pickFactory(clusterContext);

    final Map<String, String> primaryLabels = labelFactory.patroniPrimaryLabels(cluster);

    Map<String, String> annotations = Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getPostgresServices)
        .map(StackGresClusterPostgresServices::getPrimary)
        .map(StackGresClusterPostgresService::getAnnotations)
        .orElse(ImmutableMap.of());

    String serviceType = Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getPostgresServices)
        .map(StackGresClusterPostgresServices::getPrimary)
        .map(StackGresClusterPostgresService::getType)
        .orElse(StackGresClusterPostgresServiceType.CLUSTER_IP.type());

    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(name(clusterContext))
        .withLabels(primaryLabels)
        .withOwnerReferences(context.getClusterContext().getOwnerReferences())
        .withAnnotations(annotations)
        .endMetadata()
        .withNewSpec()
        .withPorts(new ServicePortBuilder()
                .withProtocol("TCP")
                .withName(PatroniConfigMap.POSTGRES_PORT_NAME)
                .withPort(PatroniUtil.POSTGRES_SERVICE_PORT)
                .withTargetPort(new IntOrString(PatroniConfigMap.POSTGRES_PORT_NAME))
                .build(),
                new ServicePortBuilder()
                .withProtocol("TCP")
                .withName(PatroniConfigMap.POSTGRES_REPLICATION_PORT_NAME)
                .withPort(PatroniUtil.REPLICATION_SERVICE_PORT)
                .withTargetPort(new IntOrString(PatroniConfigMap.POSTGRES_REPLICATION_PORT_NAME))
                .build())
        .withType(serviceType)
        .endSpec()
        .build();
  }

  private Service createPrimaryService(StackGresGeneratorContext context) {
    StackGresClusterContext clusterContext = context.getClusterContext();
    StackGresCluster cluster = clusterContext.getCluster();

    final LabelFactory<?> labelFactory = factoryDelegator.pickFactory(clusterContext);

    final Map<String, String> clusterLabels = labelFactory.clusterLabels(cluster);

    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(readWriteName(clusterContext))
        .withLabels(clusterLabels)
        .withOwnerReferences(context.getClusterContext().getOwnerReferences())
        .endMetadata()
        .withNewSpec()
        .withType("ExternalName")
        .withExternalName(name(clusterContext) + "." + cluster.getMetadata().getNamespace()
            + ".svc.cluster.local")
        .endSpec()
        .build();
  }

  private Service createReplicaService(StackGresGeneratorContext context) {
    StackGresClusterContext clusterContext = context.getClusterContext();
    StackGresCluster cluster = clusterContext.getCluster();

    final LabelFactory<?> labelFactory = factoryDelegator.pickFactory(clusterContext);
    final Map<String, String> replicaLabels = labelFactory.patroniReplicaLabels(cluster);

    Map<String, String> annotations = Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getPostgresServices)
        .map(StackGresClusterPostgresServices::getReplicas)
        .map(StackGresClusterPostgresService::getAnnotations)
        .orElse(ImmutableMap.of());

    String serviceType = Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getPostgresServices)
        .map(StackGresClusterPostgresServices::getReplicas)
        .map(StackGresClusterPostgresService::getType)
        .orElse(StackGresClusterPostgresServiceType.CLUSTER_IP.type());

    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(readOnlyName(clusterContext))
        .withLabels(replicaLabels)
        .withOwnerReferences(context.getClusterContext().getOwnerReferences())
        .withAnnotations(annotations)
        .endMetadata()
        .withNewSpec()
        .withSelector(replicaLabels)
        .withPorts(new ServicePortBuilder()
                .withProtocol("TCP")
                .withName(PatroniConfigMap.POSTGRES_PORT_NAME)
                .withPort(PatroniUtil.POSTGRES_SERVICE_PORT)
                .withTargetPort(new IntOrString(PatroniConfigMap.POSTGRES_PORT_NAME))
                .build(),
            new ServicePortBuilder()
                .withProtocol("TCP")
                .withName(PatroniConfigMap.POSTGRES_REPLICATION_PORT_NAME)
                .withPort(PatroniUtil.REPLICATION_SERVICE_PORT)
                .withTargetPort(new IntOrString(PatroniConfigMap.POSTGRES_REPLICATION_PORT_NAME))
                .build())
        .withType(serviceType)
        .endSpec()
        .build();
  }

  @Inject
  public void setFactoryDelegator(LabelFactoryDelegator factoryDelegator) {
    this.factoryDelegator = factoryDelegator;
  }
}

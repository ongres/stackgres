/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresPort;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.postgres.service.StackGresPostgresServiceType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresService;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresServices;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V_1_5)
public class PatroniServices implements
    ResourceGenerator<StackGresClusterContext> {

  private final LabelFactoryForCluster<StackGresCluster> labelFactory;

  @Inject
  public PatroniServices(LabelFactoryForCluster<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }

  public static String readWriteName(ClusterContext clusterContext) {
    return PatroniUtil.readWriteName(clusterContext.getCluster());
  }

  public static String restName(ClusterContext clusterContext) {
    return PatroniUtil.restName(clusterContext.getCluster());
  }

  public static String deprecatedReadWriteName(ClusterContext clusterContext) {
    return PatroniUtil.deprecatedReadWriteName(clusterContext.getCluster());
  }

  public static String readOnlyName(ClusterContext clusterContext) {
    return PatroniUtil.readOnlyName(clusterContext.getCluster());
  }

  public String configName(ClusterContext clusterContext) {
    return PatroniUtil.configName(clusterContext.getCluster());
  }

  /**
   * Create the Services associated with the cluster.
   */
  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getSource();
    Service config = createConfigService(context);
    Service rest = createPatroniRestService(context);
    Seq<HasMetadata> services = Seq.of(config, rest);

    if (isPrimaryServiceEnabled(cluster)) {
      services = services.append(createPrimaryService(context));
      services = services.append(createDeprecatedPrimaryService(context));
    }

    if (isReplicaServiceEnabled(cluster)) {
      services = services.append(createReplicaService(context));
    }

    return services;
  }

  private boolean isReplicaServiceEnabled(final StackGresCluster cluster) {
    return Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPostgresServices)
        .map(StackGresClusterPostgresServices::getReplicas)
        .map(StackGresPostgresService::getEnabled)
        .orElse(true);
  }

  private boolean isPrimaryServiceEnabled(final StackGresCluster cluster) {
    return Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPostgresServices)
        .map(StackGresClusterPostgresServices::getPrimary)
        .map(StackGresPostgresService::getEnabled)
        .orElse(true);
  }

  private Service createConfigService(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getSource();
    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(configName(context))
        .addToLabels(context.servicesCustomLabels())
        .addToLabels(labelFactory.clusterLabels(cluster))
        .endMetadata()
        .withNewSpec()
        .withClusterIP("None")
        .endSpec()
        .build();
  }

  private Service createPatroniRestService(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getSource();

    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(restName(context))
        .addToLabels(context.servicesCustomLabels())
        .addToLabels(labelFactory.genericLabels(cluster))
        .endMetadata()
        .withNewSpec()
        .withPorts(
            new ServicePortBuilder()
                .withProtocol("TCP")
                .withName(EnvoyUtil.PATRONI_RESTAPI_PORT_NAME)
                .withPort(EnvoyUtil.PATRONI_ENTRY_PORT)
                .withTargetPort(new IntOrString(EnvoyUtil.PATRONI_RESTAPI_PORT_NAME))
                .build())
        .withSelector(labelFactory.patroniClusterLabels(cluster))
        .withType(StackGresPostgresServiceType.CLUSTER_IP.toString())
        .endSpec()
        .build();
  }

  private Service createPrimaryService(StackGresClusterContext context) {
    StackGresCluster cluster = context.getSource();
    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(readWriteName(context))
        .addToLabels(context.servicesCustomLabels())
        .addToLabels(labelFactory.clusterLabels(cluster))
        .withAnnotations(getPrimaryServiceAnnotations(cluster))
        .endMetadata()
        .withSpec(cluster.getSpec().getPostgresServices().getPrimary())
        .editSpec()
        .addAllToPorts(List.of(
            new ServicePortBuilder()
                .withProtocol("TCP")
                .withName(EnvoyUtil.POSTGRES_PORT_NAME)
                .withPort(PatroniUtil.POSTGRES_SERVICE_PORT)
                .withTargetPort(new IntOrString(EnvoyUtil.POSTGRES_PORT_NAME))
                .build(),
            new ServicePortBuilder()
                .withProtocol("TCP")
                .withName(EnvoyUtil.POSTGRES_REPLICATION_PORT_NAME)
                .withPort(PatroniUtil.REPLICATION_SERVICE_PORT)
                .withTargetPort(new IntOrString(EnvoyUtil.POSTGRES_REPLICATION_PORT_NAME))
                .build()))
        .addAllToPorts(Seq.of(
            new ServicePortBuilder()
                .withProtocol("TCP")
                .withName(EnvoyUtil.BABELFISH_PORT_NAME)
                .withPort(PatroniUtil.BABELFISH_SERVICE_PORT)
                .withTargetPort(new IntOrString(EnvoyUtil.BABELFISH_PORT_NAME))
                .build())
            .filter(
                servicePort -> getPostgresFlavorComponent(cluster) == StackGresComponent.BABELFISH)
            .toList())
        .addAllToPorts(
            Optional.of(cluster)
            .map(StackGresCluster::getSpec)
            .map(StackGresClusterSpec::getPostgresServices)
            .map(StackGresClusterPostgresServices::getPrimary)
            .map(StackGresClusterPostgresService::getCustomPorts)
            .stream()
            .flatMap(List::stream)
            .map(ServicePortBuilder::new)
            .map(this::setCustomPort)
            .map(ServicePortBuilder::build)
            .toList())
        .endSpec()
        .build();
  }

  private Map<String, String> getPrimaryServiceAnnotations(StackGresCluster cluster) {
    return Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getPrimaryService)
        .orElse(Map.of());
  }

  private Service createDeprecatedPrimaryService(StackGresClusterContext context) {
    StackGresCluster cluster = context.getSource();

    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(deprecatedReadWriteName(context))
        .addToLabels(context.servicesCustomLabels())
        .addToLabels(labelFactory.genericLabels(cluster))
        .endMetadata()
        .withNewSpec()
        .withType("ExternalName")
        .withExternalName(readWriteName(context) + "." + cluster.getMetadata().getNamespace()
            + StackGresUtil.domainSearchPath())
        .endSpec()
        .build();
  }

  private Service createReplicaService(StackGresClusterContext context) {
    StackGresCluster cluster = context.getSource();

    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(readOnlyName(context))
        .addToLabels(context.servicesCustomLabels())
        .addToLabels(labelFactory.genericLabels(cluster))
        .withAnnotations(getReplicasServiceAnnotations(cluster))
        .endMetadata()
        .withSpec(cluster.getSpec().getPostgresServices().getReplicas())
        .editSpec()
        .withSelector(labelFactory.patroniReplicaLabels(cluster))
        .addAllToPorts(List.of(
            new ServicePortBuilder()
                .withProtocol("TCP")
                .withName(EnvoyUtil.POSTGRES_PORT_NAME)
                .withPort(PatroniUtil.POSTGRES_SERVICE_PORT)
                .withTargetPort(new IntOrString(EnvoyUtil.POSTGRES_PORT_NAME))
                .build(),
            new ServicePortBuilder()
                .withProtocol("TCP")
                .withName(EnvoyUtil.POSTGRES_REPLICATION_PORT_NAME)
                .withPort(PatroniUtil.REPLICATION_SERVICE_PORT)
                .withTargetPort(new IntOrString(EnvoyUtil.POSTGRES_REPLICATION_PORT_NAME))
                .build()))
        .addAllToPorts(Seq.of(
            new ServicePortBuilder()
                .withProtocol("TCP")
                .withName(EnvoyUtil.BABELFISH_PORT_NAME)
                .withPort(PatroniUtil.BABELFISH_SERVICE_PORT)
                .withTargetPort(new IntOrString(EnvoyUtil.BABELFISH_PORT_NAME))
                .build())
            .filter(
                servicePort -> getPostgresFlavorComponent(cluster) == StackGresComponent.BABELFISH)
            .toList())
        .addAllToPorts(
            Optional.of(cluster)
            .map(StackGresCluster::getSpec)
            .map(StackGresClusterSpec::getPostgresServices)
            .map(StackGresClusterPostgresServices::getReplicas)
            .map(StackGresClusterPostgresService::getCustomPorts)
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

  private Map<String, String> getReplicasServiceAnnotations(StackGresCluster cluster) {
    return Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getReplicasService)
        .orElse(Map.of());
  }

}

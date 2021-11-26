/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;
import static io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniConfigMap.PATRONI_RESTAPI_PORT_NAME;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.postgres.service.StackGresPostgresServiceType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresServices;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.operator.common.StackGresVersion;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V10A1, stopAt = StackGresVersion.V11)
public class PatroniServices implements
    ResourceGenerator<StackGresClusterContext> {

  private static final String NO_CLUSTER_IP = "None";

  public static final int PATRONI_SERVICE_PORT = 8008;

  private LabelFactoryForCluster<StackGresCluster> labelFactory;

  public static String name(ClusterContext clusterContext) {
    String name = clusterContext.getCluster().getMetadata().getName();
    return PatroniUtil.name(name);
  }

  public static String restName(ClusterContext clusterContext) {
    String name = clusterContext.getCluster().getMetadata().getName();
    return PatroniUtil.name(name + "-rest");
  }

  public static String readWriteName(ClusterContext clusterContext) {
    String name = clusterContext.getCluster().getMetadata().getName();
    return PatroniUtil.readWriteName(name);
  }

  public static String readOnlyName(ClusterContext clusterContext) {
    String name = clusterContext.getCluster().getMetadata().getName();
    return PatroniUtil.readOnlyName(name);
  }

  public String configName(ClusterContext clusterContext) {
    final StackGresCluster cluster = clusterContext.getCluster();
    return ResourceUtil.resourceName(
        PatroniConfigMap.clusterScope(cluster) + PatroniUtil.CONFIG_SERVICE);
  }

  /**
   * Create the Services associated with the cluster.
   */
  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getSource();
    final String namespace = cluster.getMetadata().getNamespace();

    final Map<String, String> labels = labelFactory.genericLabels(cluster);
    Service config = createConfigService(namespace, configName(context), labels);
    Service rest = createPatroniRestService(context);
    Seq<HasMetadata> services = Seq.of(config, rest);

    if (isPrimaryServiceEnabled(cluster)) {
      services = services.append(createPatroniService(context));
      services = services.append(createPrimaryService(context));
    }

    if (isReplicaServiceEnabled(cluster)) {
      services = services.append(createReplicaService(context));
    }

    return services;
  }

  private boolean isReplicaServiceEnabled(final StackGresCluster cluster) {
    boolean isReplicaServiceEnabled = Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPostgresServices)
        .map(StackGresClusterPostgresServices::getReplicas)
        .map(StackGresPostgresService::getEnabled)
        .orElse(true);
    return isReplicaServiceEnabled;
  }

  private boolean isPrimaryServiceEnabled(final StackGresCluster cluster) {
    boolean isPrimaryServiceEnabled = Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPostgresServices)
        .map(StackGresClusterPostgresServices::getPrimary)
        .map(StackGresPostgresService::getEnabled)
        .orElse(true);
    return isPrimaryServiceEnabled;
  }

  private Service createConfigService(String namespace, String serviceName,
      Map<String, String> labels) {
    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(serviceName)
        .withLabels(labels)
        .endMetadata()
        .withNewSpec()
        .withClusterIP(NO_CLUSTER_IP)
        .endSpec()
        .build();
  }

  private Service createPatroniRestService(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getSource();

    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(restName(context))
        .withLabels(labelFactory.genericLabels(cluster))
        .endMetadata()
        .withNewSpec()
        .withPorts(
            new ServicePortBuilder()
                .withProtocol("TCP")
                .withName(PATRONI_RESTAPI_PORT_NAME)
                .withPort(PATRONI_SERVICE_PORT)
                .withTargetPort(new IntOrString(PATRONI_RESTAPI_PORT_NAME))
                .build())
        .withSelector(labelFactory.patroniClusterLabels(cluster))
        .withType(StackGresPostgresServiceType.CLUSTER_IP.toString())
        .endSpec()
        .build();
  }

  private Service createPatroniService(StackGresClusterContext context) {

    StackGresCluster cluster = context.getSource();
    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(name(context))
        .withLabels(labelFactory.patroniPrimaryLabels(cluster))
        .withAnnotations(getPrimaryServiceAnnotations(cluster))
        .endMetadata()
        .withNewSpec()
        .withPorts(
            new ServicePortBuilder()
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
        .addAllToPorts(Seq.of(
            new ServicePortBuilder()
                .withProtocol("TCP")
                .withName(PatroniConfigMap.BABELFISH_PORT_NAME)
                .withPort(PatroniUtil.BABELFISH_SERVICE_PORT)
                .withTargetPort(new IntOrString(PatroniConfigMap.BABELFISH_PORT_NAME))
                .build())
            .filter(
                servicePort -> getPostgresFlavorComponent(cluster) == StackGresComponent.BABELFISH)
            .toList())
        .withType(getPrimaryServiceType(cluster))
        .withExternalIPs(getPrimaryExternalIps(cluster))
        .endSpec()
        .build();
  }

  private Map<String, String> getPrimaryServiceAnnotations(StackGresCluster cluster) {
    Map<String, String> annotations = Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getPrimaryService)
        .orElse(Map.of());
    return annotations;
  }

  private List<String> getPrimaryExternalIps(StackGresCluster cluster) {
    return Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getPostgresServices)
        .map(StackGresClusterPostgresServices::getPrimary)
        .map(StackGresPostgresService::getExternalIPs)
        .orElse(ImmutableList.of());
  }

  private String getPrimaryServiceType(StackGresCluster cluster) {
    return Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getPostgresServices)
        .map(StackGresClusterPostgresServices::getPrimary)
        .map(StackGresPostgresService::getType)
        .orElse(StackGresPostgresServiceType.CLUSTER_IP.toString());
  }

  private Service createPrimaryService(StackGresClusterContext context) {
    StackGresCluster cluster = context.getSource();

    final Map<String, String> labels = labelFactory.genericLabels(cluster);

    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(readWriteName(context))
        .withLabels(labels)
        .endMetadata()
        .withNewSpec()
        .withType("ExternalName")
        .withExternalName(name(context) + "." + cluster.getMetadata().getNamespace())
        .endSpec()
        .build();
  }

  private Service createReplicaService(StackGresClusterContext context) {

    StackGresCluster cluster = context.getSource();
    final Map<String, String> replicaLabels = labelFactory.patroniReplicaLabels(cluster);
    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(readOnlyName(context))
        .withLabels(replicaLabels)
        .withAnnotations(getReplicasServiceAnnotations(cluster))
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
        .addAllToPorts(Seq.of(
            new ServicePortBuilder()
                .withProtocol("TCP")
                .withName(PatroniConfigMap.BABELFISH_PORT_NAME)
                .withPort(PatroniUtil.BABELFISH_SERVICE_PORT)
                .withTargetPort(new IntOrString(PatroniConfigMap.BABELFISH_PORT_NAME))
                .build())
            .filter(
                servicePort -> getPostgresFlavorComponent(cluster) == StackGresComponent.BABELFISH)
            .toList())
        .withType(getReplicasServiceType(cluster))
        .withExternalIPs(getReplicasExternalIPs(cluster))
        .endSpec()
        .build();
  }

  private Map<String, String> getReplicasServiceAnnotations(StackGresCluster cluster) {
    return Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getReplicasService)
        .orElse(Map.of());
  }

  private String getReplicasServiceType(StackGresCluster cluster) {
    String serviceType = Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getPostgresServices)
        .map(StackGresClusterPostgresServices::getReplicas)
        .map(StackGresPostgresService::getType)
        .orElse(StackGresPostgresServiceType.CLUSTER_IP.toString());
    return serviceType;
  }

  private List<String> getReplicasExternalIPs(StackGresCluster cluster) {
    return Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getPostgresServices)
        .map(StackGresClusterPostgresServices::getReplicas)
        .map(StackGresPostgresService::getExternalIPs)
        .orElse(ImmutableList.of());
  }

  @Inject
  public void setLabelFactory(LabelFactoryForCluster<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }
}

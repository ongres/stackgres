/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;
import static io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniConfigMap.PATRONI_RESTAPI_PORT_NAME;

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
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V10A1, stopAt = StackGresVersion.V11)
public class PatroniServices implements
    ResourceGenerator<StackGresClusterContext> {

  public static final int PATRONI_SERVICE_PORT = 8008;

  private LabelFactoryForCluster<StackGresCluster> labelFactory;

  public static String name(ClusterContext clusterContext) {
    return PatroniUtil.name(clusterContext.getCluster());
  }

  public static String restName(ClusterContext clusterContext) {
    return PatroniUtil.restName(clusterContext.getCluster());
  }

  public static String readWriteName(ClusterContext clusterContext) {
    return PatroniUtil.readWriteName(clusterContext.getCluster());
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
    final String namespace = cluster.getMetadata().getNamespace();

    final Map<String, String> labels = labelFactory.genericLabels(cluster);

    Service config = createConfigService(namespace, configName(context),
        labels);
    Service rest = createPatroniRestService(context);

    Seq<HasMetadata> services = Seq.of(config, rest);

    boolean isPrimaryServiceEnabled = Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPostgresServices)
        .map(StackGresClusterPostgresServices::getPrimary)
        .map(StackGresPostgresService::getEnabled)
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
        .map(StackGresPostgresService::getEnabled)
        .orElse(true);

    if (isReplicaServiceEnabled) {
      Service replicas = createReplicaService(context);
      services = services.append(replicas);
    }

    return services;
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
        .withClusterIP("None")
        .endSpec()
        .build();
  }

  private Service createPatroniRestService(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getSource();

    final Map<String, String> labels = labelFactory.genericLabels(cluster);

    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(restName(context))
        .withLabels(labels)
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

    final Map<String, String> primaryLabels = labelFactory.patroniPrimaryLabels(cluster);

    Map<String, String> annotations = Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getPrimaryService)
        .orElse(Map.of());

    String serviceType = Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getPostgresServices)
        .map(StackGresClusterPostgresServices::getPrimary)
        .map(StackGresPostgresService::getType)
        .orElse(StackGresPostgresServiceType.CLUSTER_IP.toString());

    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(name(context))
        .withLabels(primaryLabels)
        .withAnnotations(annotations)
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
            .filter(servicePort -> getPostgresFlavorComponent(cluster)
                == StackGresComponent.BABELFISH)
            .toList())
        .withType(serviceType)
        .endSpec()
        .build();
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
        .withExternalName(name(context) + "." + cluster.getMetadata().getNamespace()
            + ".svc.cluster.local")
        .endSpec()
        .build();
  }

  private Service createReplicaService(StackGresClusterContext context) {

    StackGresCluster cluster = context.getSource();

    final Map<String, String> replicaLabels = labelFactory.patroniReplicaLabels(cluster);

    Map<String, String> annotations = Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .map(StackGresClusterSpecAnnotations::getReplicasService)
        .orElse(Map.of());

    String serviceType = Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getPostgresServices)
        .map(StackGresClusterPostgresServices::getReplicas)
        .map(StackGresPostgresService::getType)
        .orElse(StackGresPostgresServiceType.CLUSTER_IP.toString());

    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(readOnlyName(context))
        .withLabels(replicaLabels)
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
        .addAllToPorts(Seq.of(
            new ServicePortBuilder()
                .withProtocol("TCP")
                .withName(PatroniConfigMap.BABELFISH_PORT_NAME)
                .withPort(PatroniUtil.BABELFISH_SERVICE_PORT)
                .withTargetPort(new IntOrString(PatroniConfigMap.BABELFISH_PORT_NAME))
                .build())
            .filter(servicePort -> getPostgresFlavorComponent(cluster)
                == StackGresComponent.BABELFISH)
            .toList())
        .withType(serviceType)
        .endSpec()
        .build();
  }

  @Inject
  public void setLabelFactory(LabelFactoryForCluster<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }
}

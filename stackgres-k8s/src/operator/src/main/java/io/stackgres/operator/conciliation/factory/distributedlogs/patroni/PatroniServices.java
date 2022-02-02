/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni;

import static io.stackgres.common.crd.postgres.service.StackGresPostgresServiceType.CLUSTER_IP;

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
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.postgres.service.StackGresPostgresServices;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V12)
public class PatroniServices implements
    ResourceGenerator<StackGresDistributedLogsContext> {

  public static final int PATRONI_SERVICE_PORT = 8008;

  private LabelFactoryForCluster<StackGresDistributedLogs> labelFactory;

  public static String name(StackGresDistributedLogsContext clusterContext) {
    String name = clusterContext.getSource().getMetadata().getName();
    return PatroniUtil.name(name);
  }

  public static String readWriteName(StackGresDistributedLogsContext clusterContext) {
    String name = clusterContext.getSource().getMetadata().getName();
    return PatroniUtil.readWriteName(name);
  }

  public static String readOnlyName(StackGresDistributedLogsContext clusterContext) {
    String name = clusterContext.getSource().getMetadata().getName();
    return ResourceUtil.resourceName(name + PatroniUtil.READ_ONLY_SERVICE);
  }

  public String configName(StackGresDistributedLogsContext clusterContext) {
    final StackGresDistributedLogs cluster = clusterContext.getSource();
    final String scope = PatroniConfigMap.clusterScope(cluster);
    return ResourceUtil.resourceName(
        scope + PatroniUtil.CONFIG_SERVICE);
  }

  /**
   * Create the Services associated with the cluster.
   */
  @Override
  public Stream<HasMetadata> generateResource(StackGresDistributedLogsContext context) {
    final StackGresDistributedLogs cluster = context.getSource();
    final String namespace = cluster.getMetadata().getNamespace();

    final Map<String, String> labels = labelFactory.genericLabels(cluster);

    Service config = createConfigService(namespace, configName(context), labels);
    Service patroni = createPatroniService(context);
    Service primary = createPrimaryService(context);
    Seq<HasMetadata> services = Seq.of(config, patroni, primary);

    boolean isReplicasServiceEnabled = Optional.of(cluster)
        .map(StackGresDistributedLogs::getSpec)
        .map(StackGresDistributedLogsSpec::getPostgresServices)
        .map(StackGresPostgresServices::getReplicas)
        .map(StackGresPostgresService::getEnabled)
        .orElse(true);

    if (isReplicasServiceEnabled) {
      services = services.append(createReplicaService(context));
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

  private Service createPatroniService(StackGresDistributedLogsContext context) {
    StackGresDistributedLogs cluster = context.getSource();

    final Map<String, String> primaryLabels = labelFactory.patroniPrimaryLabels(cluster);

    String serviceType = Optional.ofNullable(cluster.getSpec())
        .map(StackGresDistributedLogsSpec::getPostgresServices)
        .map(StackGresPostgresServices::getPrimary)
        .map(StackGresPostgresService::getType)
        .orElse(CLUSTER_IP.toString());

    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(name(context))
        .withLabels(primaryLabels)
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
        .withType(serviceType)
        .endSpec()
        .build();
  }

  private Service createPrimaryService(StackGresDistributedLogsContext context) {

    StackGresDistributedLogs cluster = context.getSource();

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
            + StackGresUtil.domainSearchPath())
        .endSpec()
        .build();
  }

  private Service createReplicaService(StackGresDistributedLogsContext context) {

    StackGresDistributedLogs cluster = context.getSource();

    final Map<String, String> replicaLabels = labelFactory.patroniReplicaLabels(cluster);

    final String namespace = cluster.getMetadata().getNamespace();
    final String serviceName = readOnlyName(context);

    String serviceType = Optional.ofNullable(cluster.getSpec())
        .map(StackGresDistributedLogsSpec::getPostgresServices)
        .map(StackGresPostgresServices::getReplicas)
        .map(StackGresPostgresService::getType)
        .orElse(CLUSTER_IP.toString());

    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(serviceName)
        .withLabels(replicaLabels)
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
  public void setLabelFactory(LabelFactoryForCluster<StackGresDistributedLogs> labelFactory) {
    this.labelFactory = labelFactory;
  }
}

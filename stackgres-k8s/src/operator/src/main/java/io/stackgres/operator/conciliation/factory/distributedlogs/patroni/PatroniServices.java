/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsPostgresServices;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V_1_5)
public class PatroniServices implements
    ResourceGenerator<StackGresDistributedLogsContext> {

  public static final int PATRONI_SERVICE_PORT = 8008;

  private final LabelFactoryForCluster<StackGresDistributedLogs> labelFactory;

  @Inject
  public PatroniServices(LabelFactoryForCluster<StackGresDistributedLogs> labelFactory) {
    this.labelFactory = labelFactory;
  }

  public static String name(StackGresDistributedLogsContext clusterContext) {
    return PatroniUtil.readWriteName(clusterContext.getSource());
  }

  public static String readWriteName(StackGresDistributedLogsContext clusterContext) {
    String name = clusterContext.getSource().getMetadata().getName();
    return PatroniUtil.deprecatedReadWriteName(name);
  }

  public static String readOnlyName(StackGresDistributedLogsContext clusterContext) {
    String name = clusterContext.getSource().getMetadata().getName();
    return ResourceUtil.resourceName(name + PatroniUtil.READ_ONLY_SERVICE);
  }

  public String configName(StackGresDistributedLogsContext clusterContext) {
    return PatroniUtil.configName(clusterContext.getSource());
  }

  /**
   * Create the Services associated with the cluster.
   */
  @Override
  public Stream<HasMetadata> generateResource(StackGresDistributedLogsContext context) {
    final StackGresDistributedLogs cluster = context.getSource();

    Service config = createConfigService(context);
    Service patroni = createPrimaryService(context);
    Service primary = createDeprecatedPrimaryService(context);
    Seq<HasMetadata> services = Seq.of(config, patroni, primary);

    boolean isReplicasServiceEnabled = Optional.of(cluster)
        .map(StackGresDistributedLogs::getSpec)
        .map(StackGresDistributedLogsSpec::getPostgresServices)
        .map(StackGresDistributedLogsPostgresServices::getReplicas)
        .map(StackGresPostgresService::getEnabled)
        .orElse(true);

    if (isReplicasServiceEnabled) {
      services = services.append(createReplicaService(context));
    }

    return services;
  }

  private Service createConfigService(StackGresDistributedLogsContext context) {
    StackGresDistributedLogs cluster = context.getSource();

    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(context.getSource().getMetadata().getNamespace())
        .withName(configName(context))
        .withLabels(labelFactory.clusterLabels(cluster))
        .endMetadata()
        .withNewSpec()
        .withClusterIP("None")
        .endSpec()
        .build();
  }

  private Service createPrimaryService(StackGresDistributedLogsContext context) {
    StackGresDistributedLogs cluster = context.getSource();

    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(name(context))
        .withLabels(labelFactory.clusterLabels(cluster))
        .endMetadata()
        .withSpec(cluster.getSpec().getPostgresServices().getPrimary())
        .editSpec()
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
        .endSpec()
        .build();
  }

  private Service createDeprecatedPrimaryService(StackGresDistributedLogsContext context) {
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

    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(readOnlyName(context))
        .withLabels(labelFactory.genericLabels(cluster))
        .endMetadata()
        .withSpec(cluster.getSpec().getPostgresServices().getPrimary())
        .editSpec()
        .withSelector(labelFactory.clusterReplicaLabels(cluster))
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
        .endSpec()
        .build();
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni;

import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresServiceType;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.distributedlogs.DistributedLogsContext;
import io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniConfigMap;
import io.stackgres.operatorframework.resource.ResourceUtil;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V10)
public class PatroniServices implements
    ResourceGenerator<DistributedLogsContext> {

  private LabelFactory<StackGresDistributedLogs> labelFactory;

  public static String readWriteName(DistributedLogsContext clusterContext) {
    String name = clusterContext.getSource().getMetadata().getName();
    return PatroniUtil.readWriteName(name);
  }

  public static String readOnlyName(DistributedLogsContext clusterContext) {
    String name = clusterContext.getSource().getMetadata().getName();
    return ResourceUtil.resourceName(name + PatroniUtil.READ_ONLY_SERVICE);
  }

  public String configName(DistributedLogsContext clusterContext) {
    final StackGresDistributedLogs cluster = clusterContext.getSource();
    final String scope = labelFactory.clusterScope(cluster);
    return ResourceUtil.resourceName(
        scope + PatroniUtil.CONFIG_SERVICE);
  }

  /**
   * Create the Services associated with the cluster.
   */
  @Override
  public Stream<HasMetadata> generateResource(DistributedLogsContext context) {
    final StackGresDistributedLogs cluster = context.getSource();
    final String namespace = cluster.getMetadata().getNamespace();

    final Map<String, String> clusterLabels = labelFactory.clusterLabels(cluster);

    Service config = createConfigService(namespace, configName(context),
        clusterLabels, context);

    Service primary = createPrimaryService(context);
    Service replicas = createReplicaService(context);

    return Stream.of(config, primary, replicas);
  }

  private Service createConfigService(String namespace, String serviceName,
                                      Map<String, String> labels,
                                      DistributedLogsContext context) {
    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(serviceName)
        .withLabels(labels)
        .withOwnerReferences(context.getOwnerReferences())
        .endMetadata()
        .withNewSpec()
        .withClusterIP("None")
        .endSpec()
        .build();
  }

  private Service createPrimaryService(DistributedLogsContext context) {

    StackGresDistributedLogs cluster = context.getSource();

    final Map<String, String> primaryLabels = labelFactory.patroniPrimaryLabels(cluster);

    final String namespace = cluster.getMetadata().getNamespace();
    final String serviceName = readWriteName(context);

    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(serviceName)
        .withLabels(primaryLabels)
        .withOwnerReferences(context.getOwnerReferences())
        .endMetadata()
        .withNewSpec()
        .withSelector(primaryLabels)
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
        .withType(StackGresClusterPostgresServiceType.CLUSTER_IP.type())
        .endSpec()
        .build();
  }

  private Service createReplicaService(DistributedLogsContext context) {

    StackGresDistributedLogs cluster = context.getSource();

    final Map<String, String> replicaLabels = labelFactory.patroniReplicaLabels(cluster);

    final String namespace = cluster.getMetadata().getNamespace();
    final String serviceName = readOnlyName(context);

    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(serviceName)
        .withLabels(replicaLabels)
        .withOwnerReferences(context.getOwnerReferences())
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
        .withType(StackGresClusterPostgresServiceType.CLUSTER_IP.type())
        .endSpec()
        .build();
  }

  @Inject
  public void setLabelFactory(LabelFactory<StackGresDistributedLogs> labelFactory) {
    this.labelFactory = labelFactory;
  }
}

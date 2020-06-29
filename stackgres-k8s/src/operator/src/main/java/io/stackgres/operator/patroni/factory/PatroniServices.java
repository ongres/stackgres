/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni.factory;

import java.util.Map;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.LabelFactoryDelegator;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterResourceStreamFactory;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class PatroniServices implements StackGresClusterResourceStreamFactory {

  public static final int POSTGRES_SERVICE_PORT = 5432;
  public static final int REPLICATION_SERVICE_PORT = 5433;
  private LabelFactoryDelegator factoryDelegator;

  public static String readWriteName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getCluster().getMetadata().getName();
    return PatroniUtil.readWriteName(name);
  }

  public static String readOnlyName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getCluster().getMetadata().getName();
    return ResourceUtil.resourceName(name + PatroniUtil.READ_ONLY_SERVICE);
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
    final String namespace = cluster.getMetadata().getNamespace();

    final LabelFactory<?> labelFactory = factoryDelegator.pickFactory(clusterContext);

    final Map<String, String> clusterLabels = labelFactory.clusterLabels(cluster);
    final Map<String, String> primaryLabels = labelFactory.patroniPrimaryLabels(cluster);
    final Map<String, String> replicaLabels = labelFactory.patroniReplicaLabels(cluster);

    Service config = createConfigService(namespace, configName(clusterContext),
        clusterLabels, context);
    Service primary = createService(namespace, readWriteName(clusterContext),
        primaryLabels, context);
    Service replicas = createService(namespace, readOnlyName(clusterContext),
        replicaLabels, context);

    return Seq.of(config, primary, replicas);
  }

  private Service createConfigService(String namespace, String serviceName,
      Map<String, String> labels, StackGresGeneratorContext context) {
    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(serviceName)
        .withLabels(labels)
        .withOwnerReferences(context.getClusterContext().getOwnerReferences())
        .endMetadata()
        .withNewSpec()
        .withClusterIP("None")
        .endSpec()
        .build();
  }

  private Service createService(String namespace, String serviceName,
      Map<String, String> labels, StackGresGeneratorContext context) {

    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(serviceName)
        .withLabels(labels)
        .withOwnerReferences(context.getClusterContext().getOwnerReferences())
        .endMetadata()
        .withNewSpec()
        .withSelector(labels)
        .withPorts(new ServicePortBuilder()
                .withProtocol("TCP")
                .withName(PatroniConfigMap.POSTGRES_PORT_NAME)
                .withPort(POSTGRES_SERVICE_PORT)
                .withTargetPort(new IntOrString(PatroniConfigMap.POSTGRES_PORT_NAME))
                .build(),
            new ServicePortBuilder()
                .withProtocol("TCP")
                .withName(PatroniConfigMap.POSTGRES_REPLICATION_PORT_NAME)
                .withPort(REPLICATION_SERVICE_PORT)
                .withTargetPort(new IntOrString(PatroniConfigMap.POSTGRES_REPLICATION_PORT_NAME))
                .build())
        .withType("ClusterIP")
        .endSpec()
        .build();
  }

  @Inject
  public void setFactoryDelegator(LabelFactoryDelegator factoryDelegator) {
    this.factoryDelegator = factoryDelegator;
  }
}

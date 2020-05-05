/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni.factory;

import java.util.Map;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterResourceStreamFactory;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.sidecars.envoy.Envoy;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class PatroniServices implements StackGresClusterResourceStreamFactory {

  public static final String READ_WRITE_SERVICE = "-primary";
  public static final String READ_ONLY_SERVICE = "-replicas";
  public static final String FAILOVER_SERVICE = "-failover";
  public static final String CONFIG_SERVICE = "-config";

  public static String readWriteName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getCluster().getMetadata().getName();
    return readWriteName(name);
  }

  public static String readWriteName(String clusterName) {
    return ResourceUtil.resourceName(clusterName + READ_WRITE_SERVICE);
  }

  public static String readOnlyName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getCluster().getMetadata().getName();
    return ResourceUtil.resourceName(name + READ_ONLY_SERVICE);
  }

  public static String failoverName(StackGresClusterContext clusterContext) {
    return ResourceUtil.resourceName(
        clusterContext.clusterScope() + FAILOVER_SERVICE);
  }

  public static String configName(StackGresClusterContext clusterContext) {
    return ResourceUtil.resourceName(
        clusterContext.clusterScope() + CONFIG_SERVICE);
  }

  /**
   * Create the Services associated with the cluster.
   */
  @Override
  public Stream<HasMetadata> streamResources(StackGresGeneratorContext context) {
    final StackGresCluster cluster = context.getClusterContext().getCluster();
    final String namespace = cluster.getMetadata().getNamespace();

    Service config = createConfigService(namespace, configName(
        context.getClusterContext()), context.getClusterContext().clusterLabels(), context);
    Service primary = createService(namespace, readWriteName(context.getClusterContext()),
        context.getClusterContext().patroniPrimaryLabels(), context);
    Service replicas = createService(namespace, readOnlyName(context.getClusterContext()),
        context.getClusterContext().patroniReplicaLabels(), context);

    return Seq.of(config, primary, replicas);
  }

  private Service createConfigService(String namespace, String serviceName,
      Map<String, String> labels, StackGresGeneratorContext context) {
    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(serviceName)
        .withLabels(labels)
        .withOwnerReferences(context.getClusterContext().ownerReferences())
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
        .withOwnerReferences(context.getClusterContext().ownerReferences())
        .endMetadata()
        .withNewSpec()
        .withSelector(labels)
        .withPorts(new ServicePortBuilder()
                .withProtocol("TCP")
                .withName(PatroniConfigMap.POSTGRES_PORT_NAME)
                .withPort(Envoy.PG_ENTRY_PORT)
                .withTargetPort(new IntOrString(PatroniConfigMap.POSTGRES_PORT_NAME))
                .build(),
            new ServicePortBuilder()
                .withProtocol("TCP")
                .withName(PatroniConfigMap.POSTGRES_REPLICATION_PORT_NAME)
                .withPort(Envoy.PG_REPL_ENTRY_PORT)
                .withTargetPort(new IntOrString(PatroniConfigMap.POSTGRES_REPLICATION_PORT_NAME))
                .build())
        .withType("ClusterIP")
        .endSpec()
        .build();
  }

}

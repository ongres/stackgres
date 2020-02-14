/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni.factory;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterResourceStreamFactory;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.common.StackGresUtil;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.sidecars.envoy.Envoy;
import io.stackgres.operatorframework.resource.ResourceUtil;

import org.jooq.lambda.Seq;

@ApplicationScoped
public class PatroniServices implements StackGresClusterResourceStreamFactory {

  public static final String READ_WRITE_SERVICE = "-primary";
  public static final String READ_ONLY_SERVICE = "-replica";
  public static final String FAILOVER_SERVICE = "-failover";
  public static final String CONFIG_SERVICE = "-config";

  public static String readWriteName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getCluster().getMetadata().getName();
    return ResourceUtil.resourceName(name + READ_WRITE_SERVICE);
  }

  public static String readOnlyName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getCluster().getMetadata().getName();
    return ResourceUtil.resourceName(name + READ_ONLY_SERVICE);
  }

  public static String failoverName(StackGresClusterContext clusterContext) {
    return ResourceUtil.resourceName(
        StackGresUtil.clusterScope(clusterContext.getCluster()) + FAILOVER_SERVICE);
  }

  public static String configName(StackGresClusterContext clusterContext) {
    return ResourceUtil.resourceName(
        StackGresUtil.clusterScope(clusterContext.getCluster()) + CONFIG_SERVICE);
  }

  /**
   * Create the Services associated with the cluster.
   */
  public Stream<HasMetadata> streamResources(StackGresGeneratorContext context) {
    final StackGresCluster cluster = context.getClusterContext().getCluster();
    final String namespace = cluster.getMetadata().getNamespace();
    final Map<String, String> labels = StackGresUtil.clusterLabels(
        context.getClusterContext().getCluster());

    Service config = createConfigService(namespace, configName(
        context.getClusterContext()), labels, cluster);
    Service primary = createService(namespace, readWriteName(context.getClusterContext()),
        StackGresUtil.PRIMARY_ROLE, labels, cluster);
    Service replicas = createService(namespace, readOnlyName(context.getClusterContext()),
        StackGresUtil.REPLICA_ROLE, labels, cluster);

    return Seq.of(config, primary, replicas);
  }

  private Service createConfigService(String namespace, String serviceName,
      Map<String, String> labels, StackGresCluster cluster) {
    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(serviceName)
        .withLabels(labels)
        .withOwnerReferences(ImmutableList.of(ResourceUtil.getOwnerReference(cluster)))
        .endMetadata()
        .withNewSpec()
        .withClusterIP("None")
        .endSpec()
        .build();
  }

  private Service createService(String namespace, String serviceName, String role,
      Map<String, String> labels, StackGresCluster cluster) {
    final Map<String, String> labelsRole = new HashMap<>(labels);
    labelsRole.put(StackGresUtil.ROLE_KEY, role); // role is set by Patroni

    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(serviceName)
        .withLabels(labelsRole)
        .withOwnerReferences(ImmutableList.of(ResourceUtil.getOwnerReference(cluster)))
        .endMetadata()
        .withNewSpec()
        .withSelector(labelsRole)
        .withPorts(new ServicePortBuilder()
                .withProtocol("TCP")
                .withName(PatroniConfigMap.POSTGRES_PORT_NAME)
                .withPort(Envoy.PG_ENTRY_PORT)
                .withTargetPort(new IntOrString(PatroniConfigMap.POSTGRES_PORT_NAME))
                .build(),
            new ServicePortBuilder()
                .withProtocol("TCP")
                .withName(PatroniConfigMap.POSTGRES_REPLICATION_PORT_NAME)
                .withPort(Envoy.PG_RAW_ENTRY_PORT)
                .withTargetPort(new IntOrString(PatroniConfigMap.POSTGRES_REPLICATION_PORT_NAME))
                .build())
        .withType("ClusterIP")
        .endSpec()
        .build();
  }

}

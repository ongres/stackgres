/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.resource.ResourceUtil;
import io.stackgres.operator.sidecars.envoy.Envoy;

public class PatroniServices {

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
        ResourceUtil.clusterScope(clusterContext.getCluster()) + FAILOVER_SERVICE);
  }

  public static String configName(StackGresClusterContext clusterContext) {
    return ResourceUtil.resourceName(
        ResourceUtil.clusterScope(clusterContext.getCluster()) + CONFIG_SERVICE);
  }

  /**
   * Create the Services associated with the cluster.
   */
  public static List<Service> createServices(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getCluster();
    final String namespace = cluster.getMetadata().getNamespace();
    final Map<String, String> labels = ResourceUtil.clusterLabels(context.getCluster());

    Service config = createConfigService(namespace, configName(context), labels, cluster);
    Service primary = createService(namespace, readWriteName(context),
        ResourceUtil.PRIMARY_ROLE, labels, cluster);
    Service replicas = createService(namespace, readOnlyName(context),
        ResourceUtil.REPLICA_ROLE, labels, cluster);

    return ImmutableList.of(config, primary, replicas);
  }

  private static Service createConfigService(String namespace, String serviceName,
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

  private static Service createService(String namespace, String serviceName, String role,
      Map<String, String> labels, StackGresCluster cluster) {
    final Map<String, String> labelsRole = new HashMap<>(labels);
    labelsRole.put(ResourceUtil.ROLE_KEY, role); // role is set by Patroni

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

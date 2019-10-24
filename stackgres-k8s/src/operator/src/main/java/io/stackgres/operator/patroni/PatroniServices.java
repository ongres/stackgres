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
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.resource.ResourceUtil;

public class PatroniServices {

  public static final String READ_WRITE_SERVICE = "-primary";
  public static final String READ_ONLY_SERVICE = "-replica";
  public static final String CONFIG_SERVICE = "-config";

  /**
   * Create the Services associated with the cluster.
   */
  public static List<Service> createServices(StackGresCluster cluster) {
    final String name = cluster.getMetadata().getName();
    final String namespace = cluster.getMetadata().getNamespace();
    final Map<String, String> labels = ResourceUtil.defaultLabels(name);

    Service config = createConfigService(namespace, name + CONFIG_SERVICE, labels);
    Service primary = createService(namespace, name + READ_WRITE_SERVICE, "master", labels);
    Service replicas = createService(namespace, name + READ_ONLY_SERVICE, "replica", labels);

    return ImmutableList.of(config, primary, replicas);
  }

  private static Service createConfigService(String namespace, String serviceName,
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

  private static Service createService(String namespace, String serviceName, String role,
      Map<String, String> labels) {
    final Map<String, String> labelsRole = new HashMap<>(labels);
    labelsRole.put("role", role); // role is set by Patroni

    return new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(serviceName)
        .withLabels(labelsRole)
        .endMetadata()
        .withNewSpec()
        .withSelector(labelsRole)
        .withPorts(new ServicePortBuilder()
            .withProtocol("TCP")
            .withPort(5432)
            .withTargetPort(new IntOrString(6432))
            .build())
        .withType("LoadBalancer")
        .endSpec()
        .build();
  }

}

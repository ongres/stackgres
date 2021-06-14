/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.stackgres.apiweb.dto.cluster.KubernetesPod;
import io.stackgres.common.StackGresContext;

@ApplicationScoped
public class ClusterPodTransformer {

  private static final ImmutableMap<String, String> ANNOTATIONS_TO_COMPONENT =
      ImmutableMap.<String, String>builder()
      .put(StackGresContext.CLUSTER_CONTROLLER_VERSION_KEY, "cluster-controller")
      .put(StackGresContext.DISTRIBUTEDLOGS_CONTROLLER_VERSION_KEY, "distributedlogs-controller")
      .put(StackGresContext.POSTGRES_VERSION_KEY, "postgresql")
      .put(StackGresContext.PATRONI_VERSION_KEY, "patroni")
      .put(StackGresContext.ENVOY_VERSION_KEY, "envoy")
      .put(StackGresContext.PGBOUNCER_VERSION_KEY, "pgbouncer")
      .put(StackGresContext.PROMETHEUS_POSTGRES_EXPORTER_VERSION_KEY,
          "prometheus-postgres-exporter")
      .put(StackGresContext.FLUENTBIT_VERSION_KEY, "fluent-bit")
      .put(StackGresContext.FLUENTD_VERSION_KEY, "fluentd")
      .build();

  public KubernetesPod toResource(Pod source) {
    KubernetesPod transformation = new KubernetesPod();
    transformation.setNamespace(source.getMetadata().getNamespace());
    transformation.setName(source.getMetadata().getName());
    transformation.setRole(
        convertRole(source.getMetadata().getLabels().get(StackGresContext.ROLE_KEY)));
    if (source.getStatus() != null) {
      transformation.setIp(source.getStatus().getPodIP());
      transformation.setStatus(
          convertPhase(source.getStatus().getPhase(),
              source.getStatus().getContainerStatuses()));
      transformation.setContainers(source.getSpec()
          .getContainers().size());
      transformation.setContainersReady((int) source.getStatus()
          .getContainerStatuses()
          .stream()
          .filter(ContainerStatus::getReady)
          .count());
    }
    source.getMetadata().getAnnotations().forEach((key, value) -> {
      String component = ANNOTATIONS_TO_COMPONENT.get(key);
      if (component != null) {
        Map<String, String> componentVersions = transformation.getComponentVersions();
        if (componentVersions == null) {
          componentVersions = new HashMap<>();
          transformation.setComponentVersions(componentVersions);
        }
        componentVersions.put(component, value);
      }
    });
    return transformation;
  }

  private String convertRole(String role) {
    if (StackGresContext.PRIMARY_ROLE.equals(role)) {
      return "primary";
    }

    return role;
  }

  private String convertPhase(String phase, List<ContainerStatus> containerStatuses) {
    if ("Running".equals(phase)) {
      if (containerStatuses != null
          && containerStatuses.stream().allMatch(status -> status.getReady())) {
        return "Active";
      } else {
        return "Inactive";
      }
    }

    return phase;
  }

}

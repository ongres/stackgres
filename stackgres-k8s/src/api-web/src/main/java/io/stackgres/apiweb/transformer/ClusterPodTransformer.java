/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import static io.stackgres.common.StackGresContext.ANNOTATIONS_TO_COMPONENT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.stackgres.apiweb.dto.cluster.KubernetesPod;
import io.stackgres.common.PatroniUtil;

@ApplicationScoped
public class ClusterPodTransformer {

  public KubernetesPod toResource(Pod source) {
    KubernetesPod transformation = new KubernetesPod();
    transformation.setNamespace(source.getMetadata().getNamespace());
    transformation.setName(source.getMetadata().getName());
    transformation.setRole(
        convertRole(source.getMetadata().getLabels().get(PatroniUtil.ROLE_KEY)));
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
    if (PatroniUtil.PRIMARY_ROLE.equals(role)) {
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

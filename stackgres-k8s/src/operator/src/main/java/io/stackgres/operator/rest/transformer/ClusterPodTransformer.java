/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.transformer;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.stackgres.operator.common.StackGresUtil;
import io.stackgres.operator.rest.dto.cluster.ClusterPod;

@ApplicationScoped
public class ClusterPodTransformer {

  public ClusterPod toResource(Pod source) {
    ClusterPod transformation = new ClusterPod();
    transformation.setNamespace(source.getMetadata().getNamespace());
    transformation.setName(source.getMetadata().getName());
    transformation.setRole(
        convertRole(source.getMetadata().getLabels().get(StackGresUtil.ROLE_KEY)));
    transformation.setIp(source.getStatus().getPodIP());
    transformation.setStatus(
        convertPhase(source.getStatus().getPhase()));
    transformation.setContainers(source.getSpec()
        .getContainers().size());
    transformation.setContainersReady((int) source.getStatus()
        .getContainerStatuses()
        .stream()
        .filter(ContainerStatus::getReady)
        .count());
    return transformation;
  }

  private String convertRole(String role) {
    if (StackGresUtil.PRIMARY_ROLE.equals(role)) {
      return "primary";
    }

    return role;
  }

  private String convertPhase(String phase) {
    if ("Running".equals(phase)) {
      return "Active";
    }

    return phase;
  }

}

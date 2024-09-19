/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.stackgres.common.crd.sgcluster.StackGresCluster;

public class ObservedClusterContext {

  private final StackGresCluster cluster;

  private final List<String> podIps;

  public ObservedClusterContext(StackGresCluster cluster, List<String> podIps) {
    this.cluster = cluster;
    this.podIps = podIps;
  }

  public StackGresCluster getCluster() {
    return cluster;
  }

  public List<String> getPodIps() {
    return podIps;
  }

  public static ObservedClusterContext toObservedClusterContext(
      StackGresCluster cluster,
      List<Pod> pods) {
    return new ObservedClusterContext(
        cluster,
        pods.stream()
        .map(pod -> Optional.ofNullable(pod.getStatus())
            .map(PodStatus::getPodIP))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList());
  }

}

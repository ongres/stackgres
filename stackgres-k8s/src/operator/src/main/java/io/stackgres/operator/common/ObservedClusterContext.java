/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.stackgres.common.crd.sgcluster.StackGresCluster;

public class ObservedClusterContext {

  private final StackGresCluster cluster;

  private final List<CollectorPodContext> pods;

  public ObservedClusterContext(StackGresCluster cluster, List<CollectorPodContext> pods) {
    this.cluster = cluster;
    this.pods = pods;
  }

  public StackGresCluster getCluster() {
    return cluster;
  }

  public List<CollectorPodContext> getPods() {
    return pods;
  }

  public static ObservedClusterContext toObservedClusterContext(
      StackGresCluster cluster,
      List<Pod> pods) {
    return new ObservedClusterContext(
        cluster,
        pods.stream()
        .map(pod -> Optional.ofNullable(pod.getStatus())
            .map(PodStatus::getPodIP)
            .map(ip -> new CollectorPodContext(
                pod.getMetadata().getNamespace(),
                pod.getMetadata().getName(),
                Instant.parse(pod.getMetadata().getCreationTimestamp()),
                ip)))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList());
  }

  @Override
  public int hashCode() {
    return Objects.hash(cluster, pods);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ObservedClusterContext)) {
      return false;
    }
    ObservedClusterContext other = (ObservedClusterContext) obj;
    return Objects.equals(cluster, other.cluster) && Objects.equals(pods, other.pods);
  }

  public static class CollectorPodContext {
    
    private final String namespace;
    
    private final String name;
    
    private final Instant creationTimestamp;
    
    private final String ip;

    public CollectorPodContext(String namespace, String name, Instant creationTimestamp, String ip) {
      super();
      this.namespace = namespace;
      this.name = name;
      this.creationTimestamp = creationTimestamp;
      this.ip = ip;
    }

    public String getNamespace() {
      return namespace;
    }

    public String getName() {
      return name;
    }

    public Instant getCreationTimestamp() {
      return creationTimestamp;
    }

    public String getIp() {
      return ip;
    }

    @Override
    public int hashCode() {
      return Objects.hash(creationTimestamp, ip, name, namespace);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof CollectorPodContext)) {
        return false;
      }
      CollectorPodContext other = (CollectorPodContext) obj;
      return Objects.equals(creationTimestamp, other.creationTimestamp) && Objects.equals(ip, other.ip)
          && Objects.equals(name, other.name) && Objects.equals(namespace, other.namespace);
    }
  }

}

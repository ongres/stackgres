/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.Pod;
import io.stackgres.apiweb.configuration.WebApiProperty;
import io.stackgres.apiweb.dto.cluster.ClusterDto;
import io.stackgres.apiweb.dto.cluster.ClusterSpec;
import io.stackgres.apiweb.dto.cluster.ClusterStatus;
import io.stackgres.common.StackGresPropertyContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class ClusterTransformer
    extends AbstractResourceTransformer<ClusterDto, StackGresCluster> {

  private final StackGresPropertyContext<WebApiProperty> context;
  private final ClusterPodTransformer clusterPodTransformer;
  private final ObjectMapper mapper;

  @Inject
  public ClusterTransformer(
      StackGresPropertyContext<WebApiProperty> context,
      ClusterPodTransformer clusterPodTransformer,
      ObjectMapper mapper) {
    this.context = context;
    this.clusterPodTransformer = clusterPodTransformer;
    this.mapper = mapper;
  }

  @Override
  public StackGresCluster toCustomResource(
      @NotNull ClusterDto source,
      @Nullable StackGresCluster original) {
    StackGresCluster transformation = Optional.ofNullable(original)
        .map(o -> mapper.convertValue(original, StackGresCluster.class))
        .orElseGet(StackGresCluster::new);

    transformation.setMetadata(getCustomResourceMetadata(source, original));
    transformation.setSpec(getCustomResourceSpec(source.getSpec()));
    if (original != null) {
      if (original.getStatus() != null) {
        transformation.getStatus().setExtensions(
            original.getStatus().getExtensions());
      }
    }

    return transformation;
  }

  @Override
  public ClusterDto toDto(StackGresCluster source) {
    ClusterDto transformation = new ClusterDto();
    transformation.setMetadata(getDtoMetadata(source));
    transformation.setSpec(getResourceSpec(source.getSpec()));
    transformation.setStatus(getResourceStatus(source.getStatus()));
    transformation.setGrafanaEmbedded(isGrafanaEmbeddedEnabled());
    return transformation;
  }

  public ClusterDto toResourceWithPods(@NotNull StackGresCluster source, @Nullable List<Pod> pods) {
    ClusterDto clusterDto = toDto(source);

    clusterDto.setPods(Seq.seq(pods)
        .map(clusterPodTransformer::toResource)
        .toList());

    clusterDto.setPodsReady((int) clusterDto.getPods()
        .stream()
        .filter(pod -> pod.getContainers().equals(pod.getContainersReady()))
        .count());

    return clusterDto;
  }

  private boolean isGrafanaEmbeddedEnabled() {
    return context.getBoolean(WebApiProperty.GRAFANA_EMBEDDED);
  }

  private StackGresClusterSpec getCustomResourceSpec(ClusterSpec source) {
    return mapper.convertValue(source, StackGresClusterSpec.class);
  }

  private ClusterSpec getResourceSpec(StackGresClusterSpec source) {
    return mapper.convertValue(source, ClusterSpec.class);
  }

  private ClusterStatus getResourceStatus(StackGresClusterStatus source) {
    return mapper.convertValue(source, ClusterStatus.class);
  }

}

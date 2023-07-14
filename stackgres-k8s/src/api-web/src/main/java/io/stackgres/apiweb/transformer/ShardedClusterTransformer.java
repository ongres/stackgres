/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import static io.stackgres.common.StackGresShardedClusterForCitusUtil.getCoordinatorClusterName;
import static io.stackgres.common.StackGresShardedClusterForCitusUtil.getShardClusterName;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.stackgres.apiweb.config.WebApiProperty;
import io.stackgres.apiweb.dto.cluster.ClusterCondition;
import io.stackgres.apiweb.dto.cluster.ClusterInstalledExtension;
import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterDto;
import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterSpec;
import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterStatus;
import io.stackgres.common.StackGresPropertyContext;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class ShardedClusterTransformer
    extends AbstractResourceTransformer<ShardedClusterDto, StackGresShardedCluster> {

  private final StackGresPropertyContext<WebApiProperty> context;
  private final ObjectMapper mapper;

  @Inject
  public ShardedClusterTransformer(StackGresPropertyContext<WebApiProperty> context,
                            ObjectMapper mapper) {
    super();
    this.context = context;
    this.mapper = mapper;
  }

  @Override
  public StackGresShardedCluster toCustomResource(
      @NotNull ShardedClusterDto source,
      @Nullable StackGresShardedCluster original) {
    StackGresShardedCluster transformation = Optional.ofNullable(original)
        .map(o -> mapper.convertValue(original, StackGresShardedCluster.class))
        .orElseGet(StackGresShardedCluster::new);

    transformation.setMetadata(getCustomResourceMetadata(source, original));
    transformation.setSpec(getCustomResourceSpec(source.getSpec()));
    if (original != null && original.getStatus() != null) {
      transformation.getStatus().setToInstallPostgresExtensions(
          original.getStatus().getToInstallPostgresExtensions()
      );
    }

    return transformation;
  }

  @Override
  public ShardedClusterDto toDto(
      @NotNull StackGresShardedCluster customResource) {
    ShardedClusterDto transformation = new ShardedClusterDto();
    transformation.setMetadata(getDtoMetadata(customResource));
    transformation.setSpec(getDtoSpec(customResource.getSpec()));
    transformation.setStatus(getDtoStatus(customResource.getStatus(),
        Seq.of(getCoordinatorClusterName(customResource))
        .append(Seq.range(0, customResource.getSpec().getShards().getClusters())
            .map(index -> getShardClusterName(customResource, index)))
        .toList()));
    transformation.setGrafanaEmbedded(isGrafanaEmbeddedEnabled());
    return transformation;
  }

  private boolean isGrafanaEmbeddedEnabled() {
    return context.getBoolean(WebApiProperty.GRAFANA_EMBEDDED);
  }

  private StackGresShardedClusterSpec getCustomResourceSpec(ShardedClusterSpec source) {
    return mapper.convertValue(source, StackGresShardedClusterSpec.class);
  }

  private ShardedClusterSpec getDtoSpec(StackGresShardedClusterSpec source) {
    return mapper.convertValue(source, ShardedClusterSpec.class);
  }

  private ShardedClusterStatus getDtoStatus(StackGresShardedClusterStatus source,
      List<String> clusters) {
    if (source == null) {
      return null;
    }
    ShardedClusterStatus transformation = new ShardedClusterStatus();

    final List<Condition> sourceClusterConditions = source.getConditions();

    if (sourceClusterConditions != null) {
      transformation.setConditions(sourceClusterConditions.stream()
          .map(this::getResourceCondition)
          .collect(ImmutableList.toImmutableList()));
    }

    transformation.setToInstallPostgresExtensions(
        source.getToInstallPostgresExtensions()
        .stream()
        .map(this::getResourceInstalledExtension)
        .toList());
    transformation.setClusters(clusters);

    return transformation;
  }

  private ClusterCondition getResourceCondition(Condition source) {
    return mapper.convertValue(source, ClusterCondition.class);
  }

  private ClusterInstalledExtension getResourceInstalledExtension(
      StackGresClusterInstalledExtension source) {
    return mapper.convertValue(source, ClusterInstalledExtension.class);
  }

}

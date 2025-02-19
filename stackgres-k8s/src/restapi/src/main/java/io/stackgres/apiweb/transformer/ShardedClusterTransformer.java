/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import static io.stackgres.common.StackGresShardedClusterUtil.getCoordinatorClusterName;
import static io.stackgres.common.StackGresShardedClusterUtil.getShardClusterName;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.apiweb.configuration.WebApiProperty;
import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterDto;
import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterSpec;
import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterStatus;
import io.stackgres.common.StackGresPropertyContext;
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
  public ShardedClusterTransformer(
      StackGresPropertyContext<WebApiProperty> context,
      ObjectMapper mapper) {
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
    return transformation;
  }

  @Override
  public ShardedClusterDto toDto(
      @NotNull StackGresShardedCluster source) {
    ShardedClusterDto transformation = new ShardedClusterDto();
    transformation.setMetadata(getDtoMetadata(source));
    transformation.setSpec(getResourceSpec(source.getSpec()));
    transformation.setStatus(getResourceStatus(source.getStatus()));
    if (transformation.getStatus() == null) {
      transformation.setStatus(new ShardedClusterStatus());
    }
    transformation.getStatus().setClusters(
        Seq.of(getCoordinatorClusterName(source))
        .append(Seq.range(0, source.getSpec().getShards().getClusters())
            .map(index -> getShardClusterName(source, index)))
        .toList());
    transformation.setGrafanaEmbedded(isGrafanaEmbeddedEnabled());
    return transformation;
  }

  private boolean isGrafanaEmbeddedEnabled() {
    return context.getBoolean(WebApiProperty.GRAFANA_EMBEDDED);
  }

  private StackGresShardedClusterSpec getCustomResourceSpec(ShardedClusterSpec source) {
    return mapper.convertValue(source, StackGresShardedClusterSpec.class);
  }

  private ShardedClusterSpec getResourceSpec(StackGresShardedClusterSpec source) {
    return mapper.convertValue(source, ShardedClusterSpec.class);
  }

  private ShardedClusterStatus getResourceStatus(StackGresShardedClusterStatus source) {
    return mapper.convertValue(source, ShardedClusterStatus.class);
  }

}

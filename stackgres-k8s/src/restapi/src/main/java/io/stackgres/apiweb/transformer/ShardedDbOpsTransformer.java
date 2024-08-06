/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.apiweb.dto.shardeddbops.ShardedDbOpsDto;
import io.stackgres.apiweb.dto.shardeddbops.ShardedDbOpsSpec;
import io.stackgres.apiweb.dto.shardeddbops.ShardedDbOpsStatus;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsSpec;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ShardedDbOpsTransformer
    extends AbstractResourceTransformer<ShardedDbOpsDto, StackGresShardedDbOps> {

  private final ObjectMapper mapper;

  @Inject
  public ShardedDbOpsTransformer(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public StackGresShardedDbOps toCustomResource(
      ShardedDbOpsDto source, StackGresShardedDbOps original) {
    StackGresShardedDbOps transformation = Optional.ofNullable(original)
        .map(o -> mapper.convertValue(original, StackGresShardedDbOps.class))
        .orElseGet(StackGresShardedDbOps::new);
    transformation.setMetadata(getCustomResourceMetadata(source, original));
    transformation.setSpec(getCustomResourceSpec(source.getSpec()));
    return transformation;
  }

  @Override
  public ShardedDbOpsDto toDto(StackGresShardedDbOps source) {
    ShardedDbOpsDto transformation = new ShardedDbOpsDto();
    transformation.setMetadata(getDtoMetadata(source));
    transformation.setSpec(getResourceSpec(source.getSpec()));
    transformation.setStatus(getResourceStatus(source.getStatus()));
    return transformation;
  }

  private StackGresShardedDbOpsSpec getCustomResourceSpec(ShardedDbOpsSpec source) {
    return mapper.convertValue(source, StackGresShardedDbOpsSpec.class);
  }

  private ShardedDbOpsSpec getResourceSpec(StackGresShardedDbOpsSpec source) {
    return mapper.convertValue(source, ShardedDbOpsSpec.class);
  }

  private ShardedDbOpsStatus getResourceStatus(StackGresShardedDbOpsStatus source) {
    return mapper.convertValue(source, ShardedDbOpsStatus.class);
  }

}

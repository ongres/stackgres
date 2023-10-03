/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.apiweb.dto.shardedbackup.ShardedBackupDto;
import io.stackgres.apiweb.dto.shardedbackup.ShardedBackupSpec;
import io.stackgres.apiweb.dto.shardedbackup.ShardedBackupStatus;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupSpec;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupStatus;

@ApplicationScoped
public class ShardedBackupTransformer
    extends AbstractResourceTransformer<ShardedBackupDto, StackGresShardedBackup> {

  private final ObjectMapper mapper;

  @Inject
  public ShardedBackupTransformer(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public StackGresShardedBackup toCustomResource(
      ShardedBackupDto source, StackGresShardedBackup original) {
    StackGresShardedBackup transformation = Optional.ofNullable(original)
        .map(o -> mapper.convertValue(original, StackGresShardedBackup.class))
        .orElseGet(StackGresShardedBackup::new);
    transformation.setMetadata(getCustomResourceMetadata(source, original));
    transformation.setSpec(getCustomResourceSpec(source.getSpec()));
    return transformation;
  }

  @Override
  public ShardedBackupDto toDto(StackGresShardedBackup source) {
    ShardedBackupDto transformation = new ShardedBackupDto();
    transformation.setMetadata(getDtoMetadata(source));
    transformation.setSpec(getResourceSpec(source.getSpec()));
    transformation.setStatus(getResourceStatus(source.getStatus()));
    return transformation;
  }

  private StackGresShardedBackupSpec getCustomResourceSpec(ShardedBackupSpec source) {
    return mapper.convertValue(source, StackGresShardedBackupSpec.class);
  }

  private ShardedBackupSpec getResourceSpec(StackGresShardedBackupSpec source) {
    return mapper.convertValue(source, ShardedBackupSpec.class);
  }

  private ShardedBackupStatus getResourceStatus(StackGresShardedBackupStatus source) {
    return mapper.convertValue(source, ShardedBackupStatus.class);
  }

}

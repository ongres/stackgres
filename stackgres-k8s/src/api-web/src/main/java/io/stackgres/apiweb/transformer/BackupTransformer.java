/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.apiweb.dto.backup.BackupDto;
import io.stackgres.apiweb.dto.backup.BackupSpec;
import io.stackgres.apiweb.dto.backup.BackupStatus;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupSpec;
import io.stackgres.common.crd.sgbackup.StackGresBackupStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class BackupTransformer extends AbstractResourceTransformer<BackupDto, StackGresBackup> {

  private final ObjectMapper mapper;

  @Inject
  public BackupTransformer(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public StackGresBackup toCustomResource(BackupDto source, StackGresBackup original) {
    StackGresBackup transformation = Optional.ofNullable(original)
        .map(o -> mapper.convertValue(original, StackGresBackup.class))
        .orElseGet(StackGresBackup::new);
    transformation.setMetadata(getCustomResourceMetadata(source, original));
    transformation.setSpec(getCustomResourceSpec(source.getSpec()));
    return transformation;
  }

  @Override
  public BackupDto toDto(StackGresBackup source) {
    BackupDto transformation = new BackupDto();
    transformation.setMetadata(getDtoMetadata(source));
    transformation.setSpec(getResourceSpec(source.getSpec()));
    transformation.setStatus(getResourceStatus(source.getStatus()));
    return transformation;
  }

  private StackGresBackupSpec getCustomResourceSpec(BackupSpec source) {
    return mapper.convertValue(source, StackGresBackupSpec.class);
  }

  private BackupSpec getResourceSpec(StackGresBackupSpec source) {
    return mapper.convertValue(source, BackupSpec.class);
  }

  private BackupStatus getResourceStatus(StackGresBackupStatus source) {
    return mapper.convertValue(source, BackupStatus.class);
  }

}

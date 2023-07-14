/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.apiweb.dto.storages.BackupStorageDto;
import io.stackgres.common.crd.storages.BackupStorage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class BackupStorageTransformer implements Transformer<BackupStorageDto, BackupStorage> {

  private final ObjectMapper jsonMapper;

  @Inject
  public BackupStorageTransformer(ObjectMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
  }

  @Override
  public BackupStorageDto toTarget(BackupStorage target) {
    return jsonMapper.convertValue(target, BackupStorageDto.class);

  }

  @Override
  public BackupStorage toSource(BackupStorageDto source) {
    return jsonMapper.convertValue(source, BackupStorage.class);
  }

}

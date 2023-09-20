/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.apiweb.dto.objectstorage.ObjectStorageDto;
import io.stackgres.apiweb.dto.objectstorage.ObjectStorageStatus;
import io.stackgres.apiweb.dto.storages.BackupStorageDto;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.storages.BackupStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApplicationScoped
public class ObjectStorageTransformer
    extends AbstractDependencyResourceTransformer<ObjectStorageDto, StackGresObjectStorage> {

  private final ObjectMapper mapper;

  @Inject
  public ObjectStorageTransformer(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public StackGresObjectStorage toCustomResource(
      @NotNull ObjectStorageDto source,
      @Nullable StackGresObjectStorage original) {
    StackGresObjectStorage customResource = Optional.ofNullable(original)
        .map(o -> mapper.convertValue(original, StackGresObjectStorage.class))
        .orElseGet(StackGresObjectStorage::new);
    customResource.setMetadata(getCustomResourceMetadata(source, original));
    customResource.setSpec(getCustomResourceSpec(source.getSpec()));
    return customResource;
  }

  @Override
  public ObjectStorageDto toResource(
      @NotNull StackGresObjectStorage source,
      @NotNull List<String> clusters) {
    ObjectStorageDto dto = new ObjectStorageDto();
    dto.setMetadata(getResourceMetadata(source));
    dto.setSpec(getResourceSpec(source.getSpec()));
    dto.setStatus(new ObjectStorageStatus());
    dto.getStatus().setClusters(clusters);
    return dto;
  }

  private BackupStorage getCustomResourceSpec(BackupStorageDto source) {
    return mapper.convertValue(source, BackupStorage.class);
  }

  private BackupStorageDto getResourceSpec(BackupStorage source) {
    return mapper.convertValue(source, BackupStorageDto.class);
  }

}

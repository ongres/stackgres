/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.apiweb.dto.objectstorage.ObjectStorageDto;
import io.stackgres.apiweb.dto.objectstorage.ObjectStorageStatus;
import io.stackgres.apiweb.dto.storages.BackupStorageDto;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.storages.BackupStorage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApplicationScoped
public class ObjectStorageTransformer
    extends AbstractDependencyResourceTransformer<ObjectStorageDto, StackGresObjectStorage> {

  private final Transformer<BackupStorageDto, BackupStorage> storageTransformer;
  private final ObjectMapper mapper;

  @Inject
  public ObjectStorageTransformer(
      Transformer<BackupStorageDto, BackupStorage> storageTransformer,
      ObjectMapper mapper) {
    this.storageTransformer = storageTransformer;
    this.mapper = mapper;
  }

  @Override
  public StackGresObjectStorage toCustomResource(
      @NotNull ObjectStorageDto resource,
      @Nullable StackGresObjectStorage original
  ) {
    StackGresObjectStorage customResource = Optional.ofNullable(original)
        .map(o -> mapper.convertValue(o, StackGresObjectStorage.class))
        .orElseGet(StackGresObjectStorage::new);

    customResource.setMetadata(getCustomResourceMetadata(resource, original));
    customResource.setSpec(storageTransformer.toSource(resource.getSpec()));
    return customResource;
  }

  @Override
  public ObjectStorageDto toResource(
      @NotNull StackGresObjectStorage customResource,
      @NotNull List<String> clusters
  ) {

    ObjectStorageDto dto = new ObjectStorageDto();
    dto.setMetadata(getResourceMetadata(customResource));
    dto.setSpec(storageTransformer.toTarget(customResource.getSpec()));
    dto.setStatus(new ObjectStorageStatus());
    dto.getStatus().setClusters(clusters);
    return dto;
  }
}

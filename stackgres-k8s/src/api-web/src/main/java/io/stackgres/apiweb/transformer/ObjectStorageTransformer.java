/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

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

  private final Transformer<BackupStorageDto, BackupStorage> storageTransformer;

  public ObjectStorageTransformer(Transformer<BackupStorageDto, BackupStorage> storageTransformer) {
    this.storageTransformer = storageTransformer;
  }

  @Override
  public StackGresObjectStorage toCustomResource(
      @NotNull ObjectStorageDto resource,
      @Nullable StackGresObjectStorage original
  ) {
    StackGresObjectStorage customResource = Optional.ofNullable(original)
        .orElseGet(StackGresObjectStorage::new);

    customResource.setMetadata(getCustomResourceMetadata(resource, original));
    customResource.setSpec(storageTransformer.toTarget(resource.getSpec()));
    return customResource;
  }

  @Override
  public ObjectStorageDto toResource(
      @NotNull StackGresObjectStorage customResource,
      @NotNull List<String> clusters
  ) {

    ObjectStorageDto dto = new ObjectStorageDto();
    dto.setSpec(storageTransformer.toSource(customResource.getSpec()));
    dto.setStatus(new ObjectStorageStatus());
    dto.getStatus().setClusters(clusters);
    return dto;
  }
}

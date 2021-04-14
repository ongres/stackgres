/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.apiweb.dto.Metadata;
import io.stackgres.apiweb.dto.ResourceDto;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractDependencyResourceTransformer<T extends ResourceDto,
    R extends CustomResource<?, ?>> implements DependencyResourceTransformer<T, R> {

  protected ObjectMeta getCustomResourceMetadata(@NotNull T source, @Nullable R original) {
    ObjectMeta metadata = original != null ? original.getMetadata() : new ObjectMeta();
    if (source.getMetadata() != null) {
      metadata.setNamespace(source.getMetadata().getNamespace());
      metadata.setName(source.getMetadata().getName());
      metadata.setUid(source.getMetadata().getUid());
    }
    return metadata;
  }

  protected Metadata getResourceMetadata(@NotNull R source) {
    Metadata metadata = new Metadata();
    if (source.getMetadata() != null) {
      metadata.setNamespace(source.getMetadata().getNamespace());
      metadata.setName(source.getMetadata().getName());
      metadata.setUid(source.getMetadata().getUid());
    }
    return metadata;
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.apiweb.dto.ResourceDto;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractResourceTransformer
    <T extends ResourceDto, R extends HasMetadata>
    extends AbstractDtoTransformer<T, R> implements ResourceTransformer<T, R> {

  public ObjectMeta getCustomResourceMetadata(@NotNull T source, @Nullable R original) {
    ObjectMeta metadata = original != null ? original.getMetadata() : new ObjectMeta();
    if (source.getMetadata() != null) {
      metadata.setNamespace(source.getMetadata().getNamespace());
      metadata.setName(source.getMetadata().getName());
      metadata.setUid(source.getMetadata().getUid());
    }
    return metadata;
  }

}

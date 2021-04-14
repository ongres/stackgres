/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.apiweb.dto.Metadata;
import io.stackgres.apiweb.dto.ResourceDto;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractDtoTransformer<T extends ResourceDto, R extends CustomResource<?, ?>>
    implements DtoTransformer<T, R> {

  protected Metadata getDtoMetadata(@NotNull R source) {
    Metadata metadata = new Metadata();
    if (source.getMetadata() != null) {
      metadata.setNamespace(source.getMetadata().getNamespace());
      metadata.setName(source.getMetadata().getName());
      metadata.setUid(source.getMetadata().getUid());
    }
    return metadata;
  }

}

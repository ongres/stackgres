/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.apiweb.dto.Metadata;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.apiweb.transformer.util.TransformerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractDependencyResourceTransformer<T extends ResourceDto,
    R extends CustomResource<?, ?>> implements DependencyResourceTransformer<T, R> {

  protected ObjectMeta getCustomResourceMetadata(@NotNull T source, @Nullable R original) {
    return TransformerUtil.fromDto(
        source.getMetadata(),
        original != null ? original.getMetadata() : null);
  }

  protected Metadata getResourceMetadata(@NotNull R source) {
    return TransformerUtil.fromResource(source.getMetadata());
  }

}

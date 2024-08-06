/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.apiweb.dto.Metadata;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.apiweb.transformer.util.TransformerUtil;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractDtoTransformer<T extends ResourceDto, R extends HasMetadata>
    implements DtoTransformer<T, R> {

  protected Metadata getDtoMetadata(@NotNull R source) {
    return TransformerUtil.fromResource(source.getMetadata());
  }

}

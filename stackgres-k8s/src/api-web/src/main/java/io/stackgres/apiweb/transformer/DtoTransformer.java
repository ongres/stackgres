/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import javax.annotation.Nonnull;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.apiweb.dto.ResourceDto;

public interface DtoTransformer<T extends ResourceDto, R extends HasMetadata> {

  T toDto(@Nonnull R customResource);

}

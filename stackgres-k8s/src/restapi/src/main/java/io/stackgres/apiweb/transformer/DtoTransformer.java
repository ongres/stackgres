/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.apiweb.dto.ResourceDto;
import org.jetbrains.annotations.NotNull;

public interface DtoTransformer<T extends ResourceDto, R extends HasMetadata> {

  T toDto(@NotNull R customResource);

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.apiweb.dto.ResourceDto;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ResourceTransformer<T extends ResourceDto, R extends CustomResource<?, ?>>
    extends DtoTransformer<T, R> {

  R toCustomResource(@NotNull T resource, @Nullable R originalResource);

}

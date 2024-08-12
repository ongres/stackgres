/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import io.stackgres.apiweb.dto.ResourceDto;
import org.jetbrains.annotations.NotNull;

public interface ResourceNamespacedRestService<T extends ResourceDto> {

  @NotNull T get(@NotNull String namespace, @NotNull String name);

}

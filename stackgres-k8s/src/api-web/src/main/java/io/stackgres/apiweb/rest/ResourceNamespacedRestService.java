/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import javax.annotation.Nonnull;

import io.stackgres.apiweb.dto.ResourceDto;

public interface ResourceNamespacedRestService<T extends ResourceDto> {

  @Nonnull T get(@Nonnull String namespace, @Nonnull String name);

}

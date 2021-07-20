/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;

import io.stackgres.apiweb.dto.ResourceDto;
import org.jetbrains.annotations.NotNull;

public interface ResourceRestService<T extends ResourceDto> {

  @NotNull List<@NotNull T> list();

  void create(@NotNull T resource);

  void delete(@NotNull T resource);

  void update(@NotNull T resource);

}

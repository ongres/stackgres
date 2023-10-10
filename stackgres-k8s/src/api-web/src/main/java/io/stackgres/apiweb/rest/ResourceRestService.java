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

  T create(@NotNull T resource, Boolean dryRun);

  void delete(@NotNull T resource, Boolean dryRun);

  T update(@NotNull T resource, Boolean dryRun);

}

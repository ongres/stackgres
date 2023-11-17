/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;

import io.stackgres.apiweb.dto.ResourceDto;
import jakarta.validation.Valid;
import org.jetbrains.annotations.NotNull;

public interface ValidatedResourceRestService<T extends ResourceDto> {

  @NotNull List<@NotNull T> list();

  T create(@Valid @NotNull T resource, Boolean dryRun);

  void delete(@Valid @NotNull T resource, Boolean dryRun);

  T update(@Valid @NotNull T resource, Boolean dryRun);

}

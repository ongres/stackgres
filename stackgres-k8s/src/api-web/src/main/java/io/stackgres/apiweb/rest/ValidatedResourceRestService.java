/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;

import javax.annotation.Nonnull;

import io.stackgres.apiweb.dto.ResourceDto;
import jakarta.validation.Valid;

public interface ValidatedResourceRestService<T extends ResourceDto> {

  @Nonnull List<T> list();

  T create(@Valid @Nonnull T resource, Boolean dryRun);

  void delete(@Valid @Nonnull T resource, Boolean dryRun);

  T update(@Valid @Nonnull T resource, Boolean dryRun);

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.fabric8.kubernetes.api.model.HasMetadata;
import org.jetbrains.annotations.NotNull;

public interface ResourceWriter<T extends HasMetadata> {

  void create(@NotNull T resource);

  void update(@NotNull T resource);

  void delete(@NotNull T resource);

}

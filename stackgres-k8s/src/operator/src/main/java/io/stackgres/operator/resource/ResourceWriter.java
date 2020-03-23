/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import io.fabric8.kubernetes.api.model.HasMetadata;

public interface ResourceWriter<T extends HasMetadata> {

  void create(T resource);

  void update(T resource);

  void delete(T resource);

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.resource;

import io.fabric8.kubernetes.api.model.HasMetadata;

public interface ResourceTransactionHandler<R extends HasMetadata> {

  void create(R resource, Runnable transaction);

  void rollBack(Exception failure, R resource);
}

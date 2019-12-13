/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import io.fabric8.kubernetes.client.CustomResource;

public interface DefaultCustomResourceInitializer<T extends CustomResource> {
  void initialize();
}

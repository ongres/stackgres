/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import io.stackgres.operator.common.StackGresSidecarTransformer;

public interface SidecarFinder {

  StackGresSidecarTransformer<?> getSidecarTransformer(String name);
}

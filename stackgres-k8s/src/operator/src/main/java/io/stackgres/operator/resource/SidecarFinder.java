/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.List;

import io.stackgres.operator.common.StackGresClusterSidecarResourceFactory;

public interface SidecarFinder {

  StackGresClusterSidecarResourceFactory<?> getSidecarTransformer(String name);

  List<String> getAllOptionalSidecarNames();

}

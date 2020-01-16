/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.controller.ResourceGeneratorContext;

public interface StatefulsetResourceBuilder {

  List<HasMetadata> create(ResourceGeneratorContext<StackGresClusterContext> context);
}

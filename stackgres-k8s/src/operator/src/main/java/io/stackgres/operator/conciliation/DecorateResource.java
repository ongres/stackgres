/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.ClusterContext;

public interface DecorateResource<T extends ClusterContext> {

  List<HasMetadata> decorateResources(T context);

}

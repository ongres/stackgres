/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;

public interface ResourceGenerationDiscoverer<T> {

  List<HasMetadata> generateResources(T context);

}

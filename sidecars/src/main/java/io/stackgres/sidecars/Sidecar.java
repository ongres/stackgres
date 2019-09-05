/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.sidecars;

import java.util.List;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.sgcluster.StackGresCluster;

public interface Sidecar {

  String getName();

  Container create();

  List<HasMetadata> createDependencies(StackGresCluster resource);

}

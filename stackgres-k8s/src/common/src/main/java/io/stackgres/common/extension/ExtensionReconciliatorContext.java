/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import com.google.common.collect.ImmutableList;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;

public interface ExtensionReconciliatorContext extends ClusterContext {

  ImmutableList<StackGresClusterInstalledExtension> getExtensions();

}

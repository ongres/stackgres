/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.common;

import com.google.common.collect.ImmutableList;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.extension.ExtensionReconciliatorContext;
import io.stackgres.operatorframework.resource.ResourceHandlerContext;
import org.immutables.value.Value.Immutable;

@Immutable
public abstract class StackGresClusterContext
    implements ResourceHandlerContext, ExtensionReconciliatorContext {

  @Override
  public abstract StackGresCluster getCluster();

  @Override
  public abstract ImmutableList<StackGresClusterExtension> getExtensions();

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.configuration;

import javax.inject.Singleton;

import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.StackGresPropertyContext;
import io.stackgres.common.controller.PodLocalControllerContext;

@Singleton
public class ClusterControllerPropertyContext
    implements StackGresPropertyContext<ClusterControllerProperty>, PodLocalControllerContext {

  @Override
  public String getClusterName() {
    return getString(ClusterControllerProperty.CLUSTER_NAME);
  }

  @Override
  public String getNamespace() {
    return getString(ClusterControllerProperty.CLUSTER_NAMESPACE);
  }

  @Override
  public String getPodName() {
    return getString(ClusterControllerProperty.CLUSTER_CONTROLLER_POD_NAME);
  }
}

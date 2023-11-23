/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import java.util.Optional;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ClusterLabelMapper implements LabelMapperForCluster<StackGresCluster> {

  @Override
  public String appName() {
    return StackGresContext.CLUSTER_APP_NAME;
  }

  @Override
  public String resourceNameKey(StackGresCluster resource) {
    return getKeyPrefix(resource) + StackGresContext.CLUSTER_NAME_KEY;
  }

  @Override
  public String resourceNamespaceKey(StackGresCluster resource) {
    return getKeyPrefix(resource) + StackGresContext.CLUSTER_NAMESPACE_KEY;
  }

  @Override
  public String resourceUidKey(StackGresCluster resource) {
    return getKeyPrefix(resource) + StackGresContext.CLUSTER_UID_KEY;
  }

  @Override
  public String resourceScopeKey(StackGresCluster resource) {
    return getKeyPrefix(resource) + StackGresContext.CLUSTER_SCOPE_KEY;
  }

  @Override
  public String getKeyPrefix(StackGresCluster resource) {
    return Optional.of(resource)
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getLabelPrefix)
        .orElse(StackGresContext.STACKGRES_KEY_PREFIX);
  }

}

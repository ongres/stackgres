/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import org.immutables.value.Value.Immutable;

@Immutable
public abstract class StackGresUserClusterContext
    extends StackGresClusterContext {
  static final LabelMapper LABEL_MAPPER = new LabelMapper();

  @Override
  NestedClusterLabelMapper clusterLabelMapper() {
    return new NestedClusterLabelMapper();
  }

  public static ClusterLabelMapper getClusterLabelMapper(StackGresCluster cluster) {
    return new ClusterLabelMapper(cluster);
  }

  public static LabelMapper getLabelMapper() {
    return LABEL_MAPPER;
  }

  class NestedClusterLabelMapper extends ClusterLabelMapper {
    NestedClusterLabelMapper() {
      super(null);
    }

    @Override
    StackGresCluster clusterResource() {
      return getCluster();
    }
  }

  public static class ClusterLabelMapper
      extends StackGresClusterContext.ClusterLabelMapper<StackGresCluster> {
    final StackGresCluster cluster;

    ClusterLabelMapper(StackGresCluster cluster) {
      this.cluster = cluster;
    }

    @Override
    StackGresCluster clusterResource() {
      return cluster;
    }

    @Override
    public String appName() {
      return LABEL_MAPPER.appName();
    }

    @Override
    public String clusterNameKey() {
      return LABEL_MAPPER.clusterNameKey();
    }

    @Override
    public String clusterNamespaceKey() {
      return LABEL_MAPPER.clusterNamespaceKey();
    }

    @Override
    public String clusterUidKey() {
      return LABEL_MAPPER.clusterUidKey();
    }
  }

  public static class LabelMapper
      extends StackGresClusterContext.LabelMapper {
    @Override
    public String appName() {
      return StackGresUtil.APP_NAME;
    }

    @Override
    public String clusterNameKey() {
      return StackGresUtil.CLUSTER_NAME_KEY;
    }

    @Override
    public String clusterNamespaceKey() {
      return StackGresUtil.CLUSTER_NAMESPACE_KEY;
    }

    @Override
    public String clusterUidKey() {
      return StackGresUtil.CLUSTER_UID_KEY;
    }
  }
}

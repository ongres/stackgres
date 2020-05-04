/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import com.google.common.collect.ImmutableList;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import org.immutables.value.Value.Immutable;

@Immutable
public abstract class StackGresDistributedLogsContext
    extends StackGresClusterContext {
  static final LabelMapper LABEL_MAPPER = new LabelMapper();

  public abstract StackGresDistributedLogs getDistributedLogs();

  public abstract ImmutableList<StackGresCluster> getConnectedClusters();

  @Override
  NestedClusterLabelMapper clusterLabelMapper() {
    return new NestedClusterLabelMapper();
  }

  public static ClusterLabelMapper getClusterLabelMapper(StackGresDistributedLogs distributedLogs) {
    return new ClusterLabelMapper(distributedLogs);
  }

  public static LabelMapper getLabelMapper() {
    return new LabelMapper();
  }

  class NestedClusterLabelMapper extends ClusterLabelMapper {
    NestedClusterLabelMapper() {
      super(null);
    }

    @Override
    StackGresDistributedLogs clusterResource() {
      return getDistributedLogs();
    }
  }

  public static class ClusterLabelMapper
      extends StackGresClusterContext.ClusterLabelMapper<StackGresDistributedLogs> {
    final StackGresDistributedLogs distributedLogs;

    ClusterLabelMapper(StackGresDistributedLogs distributedLogs) {
      this.distributedLogs = distributedLogs;
    }

    @Override
    StackGresDistributedLogs clusterResource() {
      return distributedLogs;
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
      return StackGresUtil.DISTRIBUTED_LOGS_APP_NAME;
    }

    @Override
    public String clusterNameKey() {
      return StackGresUtil.DISTRIBUTED_LOGS_CLUSTER_NAME_KEY;
    }

    @Override
    public String clusterNamespaceKey() {
      return StackGresUtil.DISTRIBUTED_LOGS_CLUSTER_NAMESPACE_KEY;
    }

    @Override
    public String clusterUidKey() {
      return StackGresUtil.DISTRIBUTED_LOGS_CLUSTER_UID_KEY;
    }
  }
}

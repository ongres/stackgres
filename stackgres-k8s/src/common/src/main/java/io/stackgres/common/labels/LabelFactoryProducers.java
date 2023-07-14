/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import io.quarkus.arc.DefaultBean;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class LabelFactoryProducers {

  @Produces
  @DefaultBean
  LabelFactoryForCluster<StackGresCluster> labelForCluster(ClusterLabelFactory factory) {
    return factory;
  }

  @Produces
  @DefaultBean
  LabelFactoryForCluster<StackGresDistributedLogs>
      labelForDistributedLogs(DistributedLogsLabelFactory factory) {
    return factory;
  }

  @Produces
  @DefaultBean
  LabelFactory<StackGresBackup> labelForBackup(BackupLabelFactory factory) {
    return factory;
  }

  @Produces
  @DefaultBean
  LabelFactory<StackGresDbOps> labelForDbOps(DbOpsLabelFactory factory) {
    return factory;
  }
}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.ArcUtil;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsDefinition;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsDoneable;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsList;

@ApplicationScoped
public class DistributedLogsScanner
    extends AbstractCustomResourceScanner<StackGresDistributedLogs, StackGresDistributedLogsList,
    StackGresDistributedLogsDoneable> {

  @Inject
  public DistributedLogsScanner(KubernetesClientFactory clientFactory) {
    super(clientFactory, StackGresDistributedLogsDefinition.CONTEXT,
        StackGresDistributedLogs.class, StackGresDistributedLogsList.class,
        StackGresDistributedLogsDoneable.class);
  }

  public DistributedLogsScanner() {
    super(null, null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

}

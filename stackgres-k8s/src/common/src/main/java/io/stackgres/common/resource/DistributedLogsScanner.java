/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.CdiUtil;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsList;

@ApplicationScoped
public class DistributedLogsScanner
    extends AbstractCustomResourceScanner<StackGresDistributedLogs, StackGresDistributedLogsList> {

  @Inject
  public DistributedLogsScanner(KubernetesClientFactory clientFactory) {
    super(clientFactory, StackGresDistributedLogs.class, StackGresDistributedLogsList.class);
  }

  public DistributedLogsScanner() {
    super(null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

}

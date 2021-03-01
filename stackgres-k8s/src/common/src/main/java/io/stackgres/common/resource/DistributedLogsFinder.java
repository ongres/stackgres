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
public class DistributedLogsFinder
    extends AbstractCustomResourceFinder<StackGresDistributedLogs> {

  /**
   * Create a {@code DistributedLogsFinder} instance.
   */
  @Inject
  public DistributedLogsFinder(KubernetesClientFactory clientFactory) {
    super(clientFactory, StackGresDistributedLogs.class, StackGresDistributedLogsList.class);
  }

  public DistributedLogsFinder() {
    super(null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

}

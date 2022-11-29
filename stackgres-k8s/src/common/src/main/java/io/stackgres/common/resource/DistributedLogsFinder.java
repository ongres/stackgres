/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsList;

@ApplicationScoped
public class DistributedLogsFinder
    extends AbstractCustomResourceFinder<StackGresDistributedLogs> {

  /**
   * Create a {@code DistributedLogsFinder} instance.
   */
  @Inject
  public DistributedLogsFinder(KubernetesClient client) {
    super(client, StackGresDistributedLogs.class, StackGresDistributedLogsList.class);
  }

}

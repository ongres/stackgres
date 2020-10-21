/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.stackgres.operator.conciliation.distributedlogs.DistributedLogsContext;
import io.stackgres.operator.conciliation.factory.PodSecurityFactory;
import io.stackgres.operator.conciliation.factory.ResourceFactory;

@ApplicationScoped
public class DistributedLogsPodSecurityFactory extends PodSecurityFactory
    implements ResourceFactory<DistributedLogsContext, PodSecurityContext> {

  @Override
  public PodSecurityContext createResource(DistributedLogsContext source) {
    return createPodSecurityContext();
  }

}

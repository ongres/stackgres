/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.PodSecurityFactory;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DistributedLogsPodSecurityFactory extends PodSecurityFactory
    implements ResourceFactory<StackGresDistributedLogsContext, PodSecurityContext> {

  @Override
  public PodSecurityContext createResource(StackGresDistributedLogsContext source) {
    return createPodSecurityContext();
  }

}

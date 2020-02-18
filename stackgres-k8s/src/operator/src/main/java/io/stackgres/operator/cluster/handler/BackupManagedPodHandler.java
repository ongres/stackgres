/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.handler;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresUtil;
import io.stackgres.operator.resource.AbstractClusterResourceHandler;

@ApplicationScoped
public class BackupManagedPodHandler extends AbstractClusterResourceHandler {

  @Override
  public boolean isHandlerForResource(StackGresClusterContext context, HasMetadata resource) {
    return context != null
        && resource instanceof Pod
        && resource.getMetadata().getNamespace().equals(
            context.getCluster().getMetadata().getNamespace())
        && Objects.equals(resource.getMetadata().getLabels().get(StackGresUtil.BACKUP_KEY),
            Boolean.TRUE.toString());
  }

  @Override
  public boolean isManaged() {
    return true;
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.stackgres.operator.common.StackGresClusterConfig;
import io.stackgres.operator.resource.AbstractStackGresClusterResourceHandler;

@ApplicationScoped
public class BackupManagedCronJobPodHandler extends AbstractStackGresClusterResourceHandler {

  @Override
  public boolean isHandlerForResource(StackGresClusterConfig config, HasMetadata resource) {
    return config != null
        && resource instanceof Pod
        && resource.getMetadata().getNamespace().equals(
            config.getCluster().getMetadata().getNamespace())
        && resource.getMetadata().getName().startsWith(
            config.getCluster().getMetadata().getName()
            + StackGresStatefulSet.BACKUP_SUFFIX + "-");
  }

  @Override
  public boolean isManaged() {
    return true;
  }

}

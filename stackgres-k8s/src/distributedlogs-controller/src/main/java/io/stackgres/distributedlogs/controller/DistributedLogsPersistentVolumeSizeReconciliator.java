/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.controller;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.stackgres.common.controller.PersistentVolumeSizeReconciliator;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.distributedlogs.configuration.DistributedLogsControllerPropertyContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DistributedLogsPersistentVolumeSizeReconciliator extends
    PersistentVolumeSizeReconciliator<DistributedLogsControllerPropertyContext> {

  private final ResourceFinder<StatefulSet> stsFinder;

  private final ResourceFinder<PersistentVolumeClaim> pvcFinder;

  private final ResourceWriter<PersistentVolumeClaim> pvcWriter;

  private final DistributedLogsControllerPropertyContext podContext;

  @Inject
  public DistributedLogsPersistentVolumeSizeReconciliator(
      ResourceFinder<StatefulSet> stsFinder,
      ResourceFinder<PersistentVolumeClaim> pvcFinder,
      ResourceWriter<PersistentVolumeClaim> pvcWriter,
      DistributedLogsControllerPropertyContext podContext) {
    this.stsFinder = stsFinder;
    this.pvcFinder = pvcFinder;
    this.pvcWriter = pvcWriter;
    this.podContext = podContext;
  }

  @Override
  protected ResourceFinder<StatefulSet> getStsFinder() {
    return stsFinder;
  }

  @Override
  protected ResourceFinder<PersistentVolumeClaim> getPvcFinder() {
    return pvcFinder;
  }

  @Override
  protected ResourceWriter<PersistentVolumeClaim> getPvcWriter() {
    return pvcWriter;
  }

  @Override
  protected DistributedLogsControllerPropertyContext getComponentContext() {
    return podContext;
  }
}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.common.controller.PersistentVolumeSizeReconciliator;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClusterPersistentVolumeSizeReconciliator extends
    PersistentVolumeSizeReconciliator<ClusterControllerPropertyContext> {

  private final ResourceFinder<StatefulSet> stsFinder;

  private final ResourceFinder<PersistentVolumeClaim> pvcFinder;

  private final ResourceWriter<PersistentVolumeClaim> pvcWriter;

  @Inject
  public ClusterPersistentVolumeSizeReconciliator(
      ResourceFinder<StatefulSet> stsFinder,
      ResourceFinder<PersistentVolumeClaim> pvcFinder,
      ResourceWriter<PersistentVolumeClaim> pvcWriter) {
    this.stsFinder = stsFinder;
    this.pvcFinder = pvcFinder;
    this.pvcWriter = pvcWriter;
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

}

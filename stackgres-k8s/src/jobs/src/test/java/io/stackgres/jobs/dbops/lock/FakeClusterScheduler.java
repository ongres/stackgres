/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.lock;

import java.util.function.Consumer;

import javax.annotation.Nonnull;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.quarkus.test.Mock;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.CustomResourceScheduler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Mock
@ApplicationScoped
public class FakeClusterScheduler implements CustomResourceScheduler<StackGresCluster> {

  private final MockKubeDb kubeDb;

  @Inject
  public FakeClusterScheduler(MockKubeDb kubeDb) {
    this.kubeDb = kubeDb;
  }

  @Override
  public StackGresCluster create(@Nonnull StackGresCluster resource, boolean dryRun) {
    return kubeDb.addOrReplaceCluster(resource);
  }

  @Override
  public void delete(@Nonnull StackGresCluster resource, boolean dryRun) {
    kubeDb.delete(resource);
  }

  @Override
  public StackGresCluster update(@Nonnull StackGresCluster resource, boolean dryRun) {
    return kubeDb.addOrReplaceCluster(resource);
  }

  @Override
  public StackGresCluster update(@Nonnull StackGresCluster resource,
      @Nonnull Consumer<StackGresCluster> setter) {
    final ObjectMeta metadata = resource.getMetadata();
    var cluster = kubeDb.getCluster(metadata.getName(), metadata.getNamespace());
    setter.accept(cluster);
    return kubeDb.addOrReplaceCluster(cluster);
  }

  @Override
  public <S> StackGresCluster updateStatus(@Nonnull StackGresCluster resource,
      @Nonnull Consumer<StackGresCluster> setter) {
    final ObjectMeta metadata = resource.getMetadata();
    var cluster = kubeDb.getCluster(metadata.getName(), metadata.getNamespace());
    setter.accept(cluster);
    return kubeDb.addOrReplaceCluster(cluster);
  }

}

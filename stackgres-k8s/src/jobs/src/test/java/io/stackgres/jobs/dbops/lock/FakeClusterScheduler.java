/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.lock;

import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.quarkus.test.Mock;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.CustomResourceScheduler;
import org.jetbrains.annotations.NotNull;

@Mock
public class FakeClusterScheduler implements CustomResourceScheduler<StackGresCluster> {

  private final MockKubeDb kubeDb;

  @Inject
  public FakeClusterScheduler(MockKubeDb kubeDb) {
    this.kubeDb = kubeDb;
  }

  @Override
  public StackGresCluster create(@NotNull StackGresCluster resource) {
    return kubeDb.addOrReplaceCluster(resource);
  }

  @Override
  public StackGresCluster update(@NotNull StackGresCluster resource) {
    return kubeDb.addOrReplaceCluster(resource);
  }

  @Override
  public void delete(@NotNull StackGresCluster resource) {
    kubeDb.delete(resource);
  }

  @Override
  public StackGresCluster updateStatus(StackGresCluster resource) {
    final ObjectMeta metadata = resource.getMetadata();
    var cluster = kubeDb.getCluster(metadata.getName(), metadata.getNamespace());
    cluster.setStatus(resource.getStatus());
    return kubeDb.addOrReplaceCluster(resource);
  }

  @Override
  public <S> void updateStatus(@NotNull StackGresCluster resource,
                               @NotNull Function<StackGresCluster, S> statusGetter,
                               @NotNull BiConsumer<StackGresCluster, S> statusSetter) {
    throw new UnsupportedOperationException("not implemented");
  }
}

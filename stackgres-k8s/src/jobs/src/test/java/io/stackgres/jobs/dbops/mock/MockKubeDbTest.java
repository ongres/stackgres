/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.mock;

import java.util.function.Consumer;

import io.quarkus.test.InjectMock;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.resource.ClusterFinder;
import io.stackgres.common.resource.ClusterScheduler;
import io.stackgres.common.resource.DbOpsFinder;
import io.stackgres.common.resource.DbOpsScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

public abstract class MockKubeDbTest {

  protected MockKubeDb kubeDb;

  @InjectMock
  protected ClusterFinder clusterFinder;
  @InjectMock
  protected ClusterScheduler clusterScheduler;
  @InjectMock
  protected DbOpsFinder dbOpsFinder;
  @InjectMock
  protected DbOpsScheduler dbOpsScheduler;

  @BeforeEach
  public void steupKubeDbMocks() {
    kubeDb = new MockKubeDb();
    var mockClusterFinder = new MockClusterFinder(kubeDb);
    Mockito.lenient()
        .when(clusterFinder.findByNameAndNamespace(Mockito.any(), Mockito.any()))
        .then(invocation -> mockClusterFinder.findByNameAndNamespace(
            invocation.getArgument(0),
            invocation.getArgument(1)));
    var mockClusterScheduler = new MockClusterScheduler(kubeDb);
    Mockito.lenient()
        .when(clusterScheduler.create(Mockito.any(), Mockito.anyBoolean()))
        .then(invocation -> mockClusterScheduler.create(
            invocation.getArgument(0),
            invocation.getArgument(1)));
    Mockito
        .doAnswer(invocation -> {
          mockClusterScheduler.delete(invocation.getArgument(0));
          return null;
        })
        .when(clusterScheduler).delete(Mockito.any());
    Mockito.lenient()
        .when(clusterScheduler.update(Mockito.any()))
        .then(invocation -> mockClusterScheduler.update(
            invocation.getArgument(0)));
    Mockito.lenient()
        .when(clusterScheduler.update(Mockito.any(), Mockito.anyBoolean()))
        .then(invocation -> mockClusterScheduler.update(
            invocation.getArgument(0),
            invocation.<Boolean>getArgument(1)));
    Mockito.lenient()
        .when(clusterScheduler.update(Mockito.any(), Mockito.<Consumer<StackGresCluster>>any()))
        .then(invocation -> mockClusterScheduler.update(
            invocation.getArgument(0),
            invocation.<Consumer<StackGresCluster>>getArgument(1)));
    Mockito.lenient()
        .when(clusterScheduler.updateStatus(Mockito.any(), Mockito.<Consumer<StackGresCluster>>any()))
        .then(invocation -> mockClusterScheduler.updateStatus(
            invocation.getArgument(0),
            invocation.<Consumer<StackGresCluster>>getArgument(1)));
    var mockDbOpsFinder = new MockDbOpsFinder(kubeDb);
    Mockito.lenient()
        .when(dbOpsFinder.findByNameAndNamespace(Mockito.any(), Mockito.any()))
        .then(invocation -> mockDbOpsFinder.findByNameAndNamespace(
            invocation.getArgument(0),
            invocation.getArgument(1)));
    var mockDbOpsScheduler = new MockDbOpsScheduler(kubeDb);
    Mockito.lenient()
        .when(dbOpsScheduler.create(Mockito.any(), Mockito.anyBoolean()))
        .then(invocation -> mockDbOpsScheduler.create(
            invocation.getArgument(0),
            invocation.getArgument(1)));
    Mockito
        .doAnswer(invocation -> {
          mockDbOpsScheduler.delete(invocation.getArgument(0));
          return null;
        })
        .when(dbOpsScheduler).delete(Mockito.any());
    Mockito.lenient()
        .when(dbOpsScheduler.update(Mockito.any()))
        .then(invocation -> mockDbOpsScheduler.update(
            invocation.getArgument(0)));
    Mockito.lenient()
        .when(dbOpsScheduler.update(Mockito.any(), Mockito.anyBoolean()))
        .then(invocation -> mockDbOpsScheduler.update(
            invocation.getArgument(0),
            invocation.<Boolean>getArgument(1)));
    Mockito.lenient()
        .when(dbOpsScheduler.update(Mockito.any(), Mockito.<Consumer<StackGresDbOps>>any()))
        .then(invocation -> mockDbOpsScheduler.update(
            invocation.getArgument(0),
            invocation.<Consumer<StackGresDbOps>>getArgument(1)));
    Mockito.lenient()
        .when(dbOpsScheduler.updateStatus(Mockito.any(), Mockito.<Consumer<StackGresDbOps>>any()))
        .then(invocation -> mockDbOpsScheduler.updateStatus(
            invocation.getArgument(0),
            invocation.<Consumer<StackGresDbOps>>getArgument(1)));
  }

}

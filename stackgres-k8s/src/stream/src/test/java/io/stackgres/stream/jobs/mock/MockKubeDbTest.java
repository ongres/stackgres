/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.mock;

import java.util.function.Consumer;

import io.quarkus.test.InjectMock;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.resource.ClusterFinder;
import io.stackgres.common.resource.ClusterScheduler;
import io.stackgres.common.resource.StreamFinder;
import io.stackgres.common.resource.StreamScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

public abstract class MockKubeDbTest {

  protected MockKubeDb kubeDb;

  @InjectMock
  protected ClusterFinder clusterFinder;
  @InjectMock
  protected ClusterScheduler clusterScheduler;
  @InjectMock
  protected StreamFinder streamFinder;
  @InjectMock
  protected StreamScheduler streamScheduler;

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
    var mockStreamFinder = new MockStreamFinder(kubeDb);
    Mockito.lenient()
        .when(streamFinder.findByNameAndNamespace(Mockito.any(), Mockito.any()))
        .then(invocation -> mockStreamFinder.findByNameAndNamespace(
            invocation.getArgument(0),
            invocation.getArgument(1)));
    var mockStreamScheduler = new MockStreamScheduler(kubeDb);
    Mockito.lenient()
        .when(streamScheduler.create(Mockito.any(), Mockito.anyBoolean()))
        .then(invocation -> mockStreamScheduler.create(
            invocation.getArgument(0),
            invocation.getArgument(1)));
    Mockito
        .doAnswer(invocation -> {
          mockStreamScheduler.delete(invocation.getArgument(0));
          return null;
        })
        .when(streamScheduler).delete(Mockito.any());
    Mockito.lenient()
        .when(streamScheduler.update(Mockito.any()))
        .then(invocation -> mockStreamScheduler.update(
            invocation.getArgument(0)));
    Mockito.lenient()
        .when(streamScheduler.update(Mockito.any(), Mockito.anyBoolean()))
        .then(invocation -> mockStreamScheduler.update(
            invocation.getArgument(0),
            invocation.<Boolean>getArgument(1)));
    Mockito.lenient()
        .when(streamScheduler.update(Mockito.any(), Mockito.<Consumer<StackGresStream>>any()))
        .then(invocation -> mockStreamScheduler.update(
            invocation.getArgument(0),
            invocation.<Consumer<StackGresStream>>getArgument(1)));
    Mockito.lenient()
        .when(streamScheduler.updateStatus(Mockito.any(), Mockito.<Consumer<StackGresStream>>any()))
        .then(invocation -> mockStreamScheduler.updateStatus(
            invocation.getArgument(0),
            invocation.<Consumer<StackGresStream>>getArgument(1)));
  }

}

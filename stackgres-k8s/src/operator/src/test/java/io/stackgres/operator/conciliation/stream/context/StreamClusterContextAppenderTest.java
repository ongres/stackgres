/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.stream.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamStatusBuilder;
import io.stackgres.common.crd.sgstream.StreamStatusCondition;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.stream.StackGresStreamContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StreamClusterContextAppenderTest {

  private StreamClusterContextAppender contextAppender;

  private StackGresStream stream;

  private StackGresCluster cluster;

  @Spy
  private StackGresStreamContext.Builder contextBuilder;

  @Mock
  private CustomResourceFinder<StackGresCluster> clusterFinder;

  @BeforeEach
  void setUp() {
    stream = Fixtures.stream().loadSgClusterToCloudEvent().get();
    cluster = Fixtures.cluster().loadDefault().get();
    contextAppender = new StreamClusterContextAppender(
        clusterFinder);
  }

  @Test
  void givenStreamWithSourceCluster_shouldPass() {
    when(clusterFinder.findByNameAndNamespace(
        stream.getSpec().getSource().getSgCluster().getName(),
        stream.getMetadata().getNamespace()))
        .thenReturn(Optional.of(cluster));
    contextAppender.appendContext(stream, contextBuilder);
  }

  @Test
  void givenStreamWithoutSourceCluster_shouldFail() {
    when(clusterFinder.findByNameAndNamespace(
        stream.getSpec().getSource().getSgCluster().getName(),
        stream.getMetadata().getNamespace()))
        .thenReturn(Optional.empty());
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(stream, contextBuilder));
    assertEquals("SGCluster stackgres not found", ex.getMessage());
  }

  @Test
  void givenCompletedStreamWithoutCluster_shouldPass() {
    stream.setStatus(
        new StackGresStreamStatusBuilder()
        .withConditions(StreamStatusCondition.STREAM_COMPLETED.getCondition())
        .build());
    contextAppender.appendContext(stream, contextBuilder);
    verify(clusterFinder, Mockito.never()).findByNameAndNamespace(
        stream.getSpec().getSource().getSgCluster().getName(),
        stream.getMetadata().getNamespace());
  }

}

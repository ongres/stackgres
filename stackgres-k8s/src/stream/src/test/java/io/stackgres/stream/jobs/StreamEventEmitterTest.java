/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamStatus;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.stream.jobs.lock.MockKubeDb;
import io.stackgres.testutil.StringUtils;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

@WithKubernetesTestServer
@QuarkusTest
class StreamEventEmitterTest {

  @Inject
  public MockKubeDb kubeDb;
  public String namespace = StringUtils.getRandomNamespace();
  public String streamName = StringUtils.getRandomResourceName();
  public String clusterName = StringUtils.getRandomResourceName();
  public StackGresStream stream;
  @Inject
  StreamEventEmitter streamEventEmitter;
  @Inject
  KubernetesClient client;

  @BeforeEach
  void setUp() {

    namespace = StringUtils.getRandomNamespace();
    clusterName = StringUtils.getRandomResourceName();

    stream = Fixtures.stream().loadSgClusterToCloudEvent().get();

    stream.getMetadata().setName(streamName);
    stream.getMetadata().setNamespace(namespace);
    stream.getSpec().getSource().getSgCluster().setName(clusterName);
    stream.setStatus(new StackGresStreamStatus());

    stream = kubeDb.addOrReplaceStream(stream);

  }

  @Test
  @DisplayName("Given a Valid DbOp streamStarted should create an event")
  void givenAValidDbOpOperationStarted_ShouldCreateAnEvent() {

    streamEventEmitter.streamStarted(streamName, namespace);

    assertEvent(StreamEvents.STREAM_STARTED,
        "Stream started");

  }

  @Test
  @DisplayName("Given a Valid DbOp streamCompleted should create an event")
  void givenAValidDbOpOperationCompleted_shouldCreateAnEvent() {
    streamEventEmitter.streamCompleted(streamName, namespace);

    assertEvent(StreamEvents.STREAM_COMPLETED,
        "Stream completed");
  }

  @Test
  @DisplayName("Given a Valid DbOp streamFailed should create an event")
  void givenAValidDbOpstreamFailed_shouldCreateAnEvent() {
    streamEventEmitter.streamFailed(streamName, namespace);

    assertEvent(StreamEvents.STREAM_FAILED,
        "Stream failed");
  }

  @Test
  @DisplayName("Given a Valid DbOp streamTimeout should create an event")
  void givenAValidDbOpPperationTimedOut_shouldCreateAnEvent() {
    streamEventEmitter.streamTimedOut(streamName, namespace);

    assertEvent(StreamEvents.STREAM_TIMEOUT,
        "Stream timed out");
  }

  private void assertEvent(StreamEvents dbOpEvent, String message) {

    var actualEvent = client.v1().events()
            .inNamespace(namespace)
            .list().getItems()
        .stream()
        .filter(event -> event.getReason().equals(dbOpEvent.reason()))
        .findAny()
        .orElseThrow(() -> new AssertionFailedError("The event was not created"));

    assertEquals(message, actualEvent.getMessage());

    assertEquals(dbOpEvent.reason(), actualEvent.getReason());

    assertEquals(dbOpEvent.type().type(), actualEvent.getType());

    assertInvolvedObject(actualEvent);

  }

  private void assertInvolvedObject(Event event) {
    StackGresStream stream = kubeDb.getStream(streamName, namespace);

    final ObjectReference involvedObject = event.getInvolvedObject();
    final ObjectMeta metadata = stream.getMetadata();
    assertEquals(stream.getKind(), involvedObject.getKind());
    assertEquals(stream.getApiVersion(), involvedObject.getApiVersion());
    assertEquals(metadata.getName(), involvedObject.getName());
    assertEquals(metadata.getUid(), involvedObject.getUid());
    assertEquals(metadata.getNamespace(), involvedObject.getNamespace());
    assertEquals(metadata.getResourceVersion(), involvedObject.getResourceVersion());
  }
}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.jobs.dbops.lock.MockKubeDb;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

@WithKubernetesTestServer
@QuarkusTest
class DatabaseOperationEventEmitterImplTest {

  @Inject
  public MockKubeDb kubeDb;
  public String namespace = StringUtils.getRandomNamespace();
  public String dbOpsName = StringUtils.getRandomClusterName();
  public String clusterName = StringUtils.getRandomClusterName();
  public StackGresDbOps dbOps;
  @Inject
  DatabaseOperationEventEmitterImpl databaseOperationEventEmitter;
  @Inject
  KubernetesClient client;

  @BeforeEach
  void setUp() {

    namespace = StringUtils.getRandomNamespace();
    clusterName = StringUtils.getRandomClusterName();

    dbOps = Fixtures.dbOps().loadSecurityUpgrade().get();

    dbOps.getMetadata().setName(dbOpsName);
    dbOps.getMetadata().setNamespace(namespace);
    dbOps.getSpec().setSgCluster(clusterName);
    dbOps.setStatus(new StackGresDbOpsStatus());
    dbOps.getStatus().setOpRetries(0);
    dbOps.getStatus().setOpStarted(Instant.now().toString());
    dbOps.getSpec().setOp("securityUpgrade");

    dbOps = kubeDb.addOrReplaceDbOps(dbOps);

  }

  @Test
  @DisplayName("Given a Valid DbOp operationStarted should create an event")
  void givenAValidDbOpOperationStarted_ShouldCreateAnEvent() {

    databaseOperationEventEmitter.operationStarted(dbOpsName, namespace);

    assertEvent(DbOpsEvents.DB_OP_STARTED,
        "Database operation " + dbOps.getSpec().getOp() + " started");

  }

  @Test
  @DisplayName("Given a Valid DbOp operationCompleted should create an event")
  void givenAValidDbOpOperationCompleted_shouldCreateAnEvent() {
    databaseOperationEventEmitter.operationCompleted(dbOpsName, namespace);

    assertEvent(DbOpsEvents.DB_OP_COMPLETED,
        "Database operation " + dbOps.getSpec().getOp() + " completed");
  }

  @Test
  @DisplayName("Given a Valid DbOp operationFailed should create an event")
  void givenAValidDbOpoperationFailed_shouldCreateAnEvent() {
    databaseOperationEventEmitter.operationFailed(dbOpsName, namespace);

    assertEvent(DbOpsEvents.DB_OP_FAILED,
        "Database operation " + dbOps.getSpec().getOp() + " failed");
  }

  @Test
  @DisplayName("Given a Valid DbOp operationTimeout should create an event")
  void givenAValidDbOpPperationTimedOut_shouldCreateAnEvent() {
    databaseOperationEventEmitter.operationTimedOut(dbOpsName, namespace);

    assertEvent(DbOpsEvents.DB_OP_TIMEOUT,
        "Database operation " + dbOps.getSpec().getOp() + " timed out");
  }

  private void assertEvent(DbOpsEvents dbOpEvent, String message) {

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
    StackGresDbOps dbOps = kubeDb.getDbOps(dbOpsName, namespace);

    final ObjectReference involvedObject = event.getInvolvedObject();
    final ObjectMeta metadata = dbOps.getMetadata();
    assertEquals(dbOps.getKind(), involvedObject.getKind());
    assertEquals(dbOps.getApiVersion(), involvedObject.getApiVersion());
    assertEquals(metadata.getName(), involvedObject.getName());
    assertEquals(metadata.getUid(), involvedObject.getUid());
    assertEquals(metadata.getNamespace(), involvedObject.getNamespace());
    assertEquals(metadata.getResourceVersion(), involvedObject.getResourceVersion());
  }
}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.cluster.controller.PatroniOperationReconciliator.RestartAction;
import org.jooq.lambda.Unchecked;
import org.junit.jupiter.api.Test;

class PatroniOperationReconciliatorTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final Duration RESTART_DELAY = Duration.ofMinutes(5);

  private static final Instant NOW = Instant.parse("2026-09-18T10:00:00Z");

  @Test
  void givenAnotherOperation_shouldDoNothing() {
    assertEquals(RestartAction.NONE, getRestartAction("{\"type\":\"switchover\"}"));
    assertEquals(RestartAction.NONE, getRestartAction("{}"));
  }

  @Test
  void givenARestartOperationNotPerformedYet_shouldRestart() {
    assertEquals(RestartAction.RESTART,
        getRestartAction("{\"type\":\"restart\",\"issued\":\"2026-09-18T09:59:00Z\"}"));
  }

  @Test
  void givenARestartOperationPerformedWithinTheRestartDelay_shouldWait() {
    assertEquals(RestartAction.WAIT,
        getRestartAction("{\"type\":\"restart\",\"started\":\"2026-09-18T09:58:00Z\"}"));
  }

  @Test
  void givenARestartOperationPerformedBeforeTheRestartDelay_shouldComplete() {
    assertEquals(RestartAction.COMPLETE,
        getRestartAction("{\"type\":\"restart\",\"started\":\"2026-09-18T09:54:59Z\"}"));
  }

  @Test
  void givenARestartOperationWithAnInvalidStartedInstant_shouldRestart() {
    assertEquals(RestartAction.RESTART,
        getRestartAction("{\"type\":\"restart\",\"started\":\"yesterday\"}"));
  }

  private RestartAction getRestartAction(String patroniOperation) {
    return PatroniOperationReconciliator.getRestartAction(
        readTree(patroniOperation), RESTART_DELAY, NOW);
  }

  private JsonNode readTree(String patroniOperation) {
    return Unchecked.supplier(() -> OBJECT_MAPPER.readTree(patroniOperation)).get();
  }

}

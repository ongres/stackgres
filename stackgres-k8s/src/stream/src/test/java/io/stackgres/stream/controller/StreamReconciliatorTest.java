/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;

import io.stackgres.common.crd.sgstream.StackGresStreamStreamingStatus;
import io.stackgres.stream.app.StreamMBeamMonitor.StreamMBeanInfo;
import io.stackgres.stream.jobs.Metrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StreamReconciliatorTest {

  @Mock
  private Metrics metrics;

  @Mock
  private StreamMBeanInfo streamMBeanInfo;

  private StreamReconciliator reconciliator;

  @BeforeEach
  void setUp() {
    StreamReconciliator.Parameters parameters = new StreamReconciliator.Parameters();
    parameters.metrics = metrics;
    reconciliator = new StreamReconciliator(parameters);
  }

  private MBeanInfo mbeanInfoOf(String... attributeNames) {
    return new MBeanInfo(
        StackGresStreamStreamingStatus.class.getName(),
        null,
        Arrays.stream(attributeNames)
            .map(attributeName -> new MBeanAttributeInfo(
                attributeName, Long.class.getName(), null, true, false, false))
            .toArray(MBeanAttributeInfo[]::new),
        null,
        null,
        null);
  }

  @Test
  void givenAttributesInAnyOrder_shouldSetEachFieldFromTheMatchingAttribute() throws Exception {
    // The attributes are deliberately not in the order in which the fields are declared: the
    // number of committed transactions is declared after the counters of events
    Map<String, Object> attributes = new LinkedHashMap<>();
    attributes.put("NumberOfCommittedTransactions", 2L);
    attributes.put("TotalNumberOfDeleteEventsSeen", 0L);
    attributes.put("Connected", Boolean.TRUE);
    attributes.put("TotalNumberOfCreateEventsSeen", 201L);
    attributes.put("CapturedTables", new String[] {"public.test"});
    attributes.put("TotalNumberOfUpdateEventsSeen", 3L);

    StackGresStreamStreamingStatus status = new StackGresStreamStreamingStatus();

    reconciliator.setStatusMetrics(status, StackGresStreamStreamingStatus.class, attributes);

    assertEquals(Long.valueOf(2L), status.getNumberOfCommittedTransactions());
    assertEquals(Long.valueOf(201L), status.getTotalNumberOfCreateEventsSeen());
    assertEquals(Long.valueOf(3L), status.getTotalNumberOfUpdateEventsSeen());
    assertEquals(Long.valueOf(0L), status.getTotalNumberOfDeleteEventsSeen());
    assertEquals(Boolean.TRUE, status.getConnected());
    assertEquals(List.of("public.test"), status.getCapturedTables());
  }

  @Test
  void givenAnAttributeThatIsNotRead_shouldLeaveItsFieldUnset() throws Exception {
    Map<String, Object> attributes = Map.of("TotalNumberOfCreateEventsSeen", 201L);

    StackGresStreamStreamingStatus status = new StackGresStreamStreamingStatus();

    reconciliator.setStatusMetrics(status, StackGresStreamStreamingStatus.class, attributes);

    assertEquals(Long.valueOf(201L), status.getTotalNumberOfCreateEventsSeen());
    assertNull(status.getNumberOfCommittedTransactions());
    assertNull(status.getTotalNumberOfEventsSeen());
  }

  @Test
  void givenAnMBean_shouldAskForTheAttributesOfTheFieldsOfTheStatusSectionOnly() {
    when(streamMBeanInfo.getMbeanInfo()).thenReturn(mbeanInfoOf(
        "NumberOfCommittedTransactions",
        "AnAttributeOfTheMBeanThatIsNotInTheStatus",
        "TotalNumberOfCreateEventsSeen"));

    Set<String> attributeNames = StreamReconciliator
        .getAttributeNames(StackGresStreamStreamingStatus.class, streamMBeanInfo)
        .stream()
        .collect(Collectors.toSet());

    assertEquals(
        Set.of("NumberOfCommittedTransactions", "TotalNumberOfCreateEventsSeen"),
        attributeNames);
  }

}

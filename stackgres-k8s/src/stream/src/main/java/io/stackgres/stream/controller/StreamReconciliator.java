/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.controller;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamEventsStatus;
import io.stackgres.common.crd.sgstream.StackGresStreamSnapshotStatus;
import io.stackgres.common.crd.sgstream.StackGresStreamStatus;
import io.stackgres.common.crd.sgstream.StackGresStreamStreamingStatus;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import io.stackgres.operatorframework.reconciliation.Reconciliator;
import io.stackgres.stream.common.StackGresStreamContext;
import io.stackgres.stream.configuration.StreamPropertyContext;
import io.stackgres.stream.jobs.Metrics;
import io.stackgres.stream.jobs.source.SgClusterDebeziumEngineHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class StreamReconciliator
    extends Reconciliator<StackGresStreamContext> {

  private static final Logger LOGGER = LoggerFactory.getLogger(StreamReconciliator.class);

  private final Metrics metrics;
  private final CustomResourceScheduler<StackGresStream> streamScheduler;

  @Inject
  public StreamReconciliator(Parameters parameters) {
    this.metrics = parameters.metrics;
    this.streamScheduler = parameters.streamScheduler;
  }

  public StreamReconciliator() {
    super();
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.metrics = null;
    this.streamScheduler = null;
  }

  @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION",
      justification = "False positives")
  @Override
  public ReconciliationResult<Void> reconcile(KubernetesClient client,
      StackGresStreamContext context) throws Exception {
    final StackGresStream stream = context.getStream();
    if (stream.getStatus() == null) {
      stream.setStatus(new StackGresStreamStatus());
    }
    var platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
    if (stream.getStatus().getSnapshot() == null) {
      stream.getStatus().setSnapshot(new StackGresStreamSnapshotStatus());
    }
    StackGresStreamSnapshotStatus snapshotStatus = stream.getStatus().getSnapshot();
    setStatusMetrics(
        stream,
        snapshotStatus,
        StackGresStreamSnapshotStatus.class,
        "debezium.postgres:type=connector-metrics,context=snapshot,server="
            + SgClusterDebeziumEngineHandler.topicPrefix(stream),
        platformMBeanServer);
    if (stream.getStatus().getStreaming() == null) {
      stream.getStatus().setStreaming(new StackGresStreamStreamingStatus());
    }
    StackGresStreamStreamingStatus streamingStatus = stream.getStatus().getStreaming();
    setStatusMetrics(
        stream,
        streamingStatus,
        StackGresStreamStreamingStatus.class,
        "debezium.postgres:type=connector-metrics,context=streaming,server="
            + SgClusterDebeziumEngineHandler.topicPrefix(stream),
        platformMBeanServer);
    if (stream.getStatus().getEvents() == null) {
      stream.getStatus().setEvents(new StackGresStreamEventsStatus());
    }
    stream.getStatus().getEvents().setLastEventWasSent(metrics.isLastEventWasSent());
    stream.getStatus().getEvents().setLastEventSent(metrics.getLastEventSent());
    stream.getStatus().getEvents().setTotalNumberOfEventsSent(metrics.getTotalNumberOfEventsSent());
    stream.getStatus().getEvents().setLastErrorSeen(metrics.getLastErrorSeen());
    stream.getStatus().getEvents().setTotalNumberOfErrorsSeen(metrics.getTotalNumberOfErrorsSeen());
    streamScheduler.update(stream, Unchecked.consumer(
        currentStream -> currentStream.setStatus(stream.getStatus())));
    return new ReconciliationResult<Void>();
  }

  private void setStatusMetrics(
      StackGresStream currentStream,
      Object statusSection,
      Class<?> statusSectionClass,
      String mbeanName,
      MBeanServer platformMBeanServer)
      throws Exception {
    try {
      ObjectName sectionMetricsName = new ObjectName(mbeanName);
      var sectionMetricsMBean = platformMBeanServer.getMBeanInfo(sectionMetricsName);
      for (Field field : statusSectionClass.getDeclaredFields()) {
        String attributeName = field.getName().substring(0, 1).toUpperCase(Locale.US)
            + field.getName().substring(1);
        String setterMethodName = "set" + attributeName;
        Method setterMethod = statusSectionClass.getMethod(setterMethodName, field.getType());
        for (var attribute : sectionMetricsMBean.getAttributes()) {
          if (attribute.getName().equals(attributeName)) {
            Object attributeValue = platformMBeanServer.getAttribute(sectionMetricsName, attributeName);
            if (attributeValue instanceof String[] attributeValueStringArray) {
              attributeValue = Arrays.asList(attributeValueStringArray);
            } else if (attributeValue instanceof Map attributeValueMap) {
              Map<?, ?> attributeValueMapGeneric = attributeValueMap;
              attributeValue = Seq.<Object, Object>seq(attributeValueMapGeneric)
                  .flatMap(t -> {
                    if (t.v1 instanceof List keyList
                        && t.v2 instanceof CompositeDataSupport cdsValues
                        && cdsValues.getCompositeType().keySet().contains("value")) {
                      return ((List<?>) keyList).stream()
                          .map(key -> Tuple.tuple(key, cdsValues.get("value")));
                    }
                    return Seq.of(t.map1(Object::toString).map2(Object::toString));
                  })
                  .toMap(Tuple2::v1, Tuple2::v2);
            }
            setterMethod.invoke(statusSection, attributeValue);
            if (attributeValue instanceof Number attributeValueNumber) {
              metrics.gauge(attributeName, attributeValueNumber);
            }
            break;
          }
        }
      }
    } catch (InstanceNotFoundException ex) {
      LOGGER.debug("Error occurred while trying to retrieve MBean " + mbeanName, ex);
      return;
    }
  }

  @Dependent
  public static class Parameters {
    @Inject Metrics metrics;
    @Inject CustomResourceScheduler<StackGresStream> streamScheduler;
    @Inject StreamPropertyContext propertyContext;
  }

}

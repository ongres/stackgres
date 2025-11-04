/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.app;

import java.lang.management.ManagementFactory;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.stream.controller.StreamReconciliationCycle;
import io.stackgres.stream.jobs.source.SgClusterDebeziumEngineHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class StreamMBeamMonitor {

  private static final Logger LOGGER = LoggerFactory.getLogger(StreamMBeamMonitor.class);

  private final CustomResourceFinder<StackGresStream> streamFinder;
  private final ScheduledExecutorService scheduledExecutorService;
  private final AtomicReference<StreamMBeanInfo> snapshotMBean =
      new AtomicReference<>();
  private final AtomicReference<StreamMBeanInfo> streamingMBean =
      new AtomicReference<>();
  private final StreamReconciliationCycle streamReconciliationCycle;
  private final MBeanServer platformMBeanServer;

  @Inject
  public StreamMBeamMonitor(
      CustomResourceFinder<StackGresStream> streamFinder,
      StreamReconciliationCycle streamReconciliationCycle) {
    this.streamFinder = streamFinder;
    this.scheduledExecutorService =
        Executors.newScheduledThreadPool(1, r -> new Thread(r, "StreamMBeamMonitor"));
    this.streamReconciliationCycle = streamReconciliationCycle;
    this.platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
  }

  protected int getPeriod() {
    return StreamProperty.STREAM_MBEAN_POLLING_PERIOD
        .get()
        .map(Integer::valueOf)
        .orElse(3);
  }

  public boolean hasData() {
    return snapshotMBean.get() != null && streamingMBean.get() != null;
  }

  public StreamMBeanInfo getSnapshotMBean() {
    return snapshotMBean.get();
  }

  public StreamMBeanInfo getStreamingMBean() {
    return streamingMBean.get();
  }

  public void start(String streamNamespace, String streamName) throws Exception {
    var stream = streamFinder.findByNameAndNamespace(streamName, streamNamespace)
        .orElseThrow(() -> new RuntimeException("Can not find SGStream "
            + streamNamespace + "." + streamName));
    scheduledExecutorService.schedule(() -> update(stream), getPeriod(), TimeUnit.SECONDS);
  }

  private void update(StackGresStream stream) {
    try {
      final String topicPrefix = SgClusterDebeziumEngineHandler.topicPrefix(stream);
      final var snapshotObjectName = new ObjectName(
          "debezium.postgres:type=connector-metrics,context=snapshot,server=" + topicPrefix);
      final var streamingObjectName = new ObjectName(
          "debezium.postgres:type=connector-metrics,context=streaming,server=" + topicPrefix);
      var previousSnapshotMBean = snapshotMBean.get();
      var previousStreamingMBean = streamingMBean.get();
      snapshotMBean.set(new StreamMBeanInfo(
          snapshotObjectName,
          platformMBeanServer.getMBeanInfo(snapshotObjectName)));
      streamingMBean.set(new StreamMBeanInfo(
          streamingObjectName,
          platformMBeanServer.getMBeanInfo(streamingObjectName)));
      if (!Objects.equals(snapshotMBean.get(), previousSnapshotMBean)
          || !Objects.equals(streamingMBean.get(), previousStreamingMBean)) {
        streamReconciliationCycle.reconcileAll();
      }
    } catch (InstanceNotFoundException ex) {
      LOGGER.trace("Error while retrieving MBean stats", ex);
    } catch (Throwable ex) {
      LOGGER.error("Error while retrieving MBean stats", ex);
    }
    scheduledExecutorService.schedule(() -> update(stream), getPeriod(), TimeUnit.SECONDS);
  }

  public class StreamMBeanInfo {
    private final ObjectName objectName;

    private final MBeanInfo mbeanInfo;

    StreamMBeanInfo(ObjectName objectName, MBeanInfo mbeanInfo) {
      this.objectName = objectName;
      this.mbeanInfo = mbeanInfo;
    }

    public ObjectName getObjectName() {
      return objectName;
    }

    public MBeanInfo getMbeanInfo() {
      return mbeanInfo;
    }

    public Object getAttribute(String attributeName) throws Exception {
      return platformMBeanServer.getAttribute(objectName, attributeName);
    }
  }

}

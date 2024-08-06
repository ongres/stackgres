/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.stream;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.Condition;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class StreamStatus {

  private List<Condition> conditions = new ArrayList<>();

  private StreamSnapshotStatus snapshot;

  private StreamStreamingStatus streaming;

  private StreamEventsStatus events;

  private String failure;

  public List<Condition> getConditions() {
    return conditions;
  }

  public void setConditions(List<Condition> conditions) {
    this.conditions = conditions;
  }

  public StreamSnapshotStatus getSnapshot() {
    return snapshot;
  }

  public void setSnapshot(StreamSnapshotStatus snapshot) {
    this.snapshot = snapshot;
  }

  public StreamStreamingStatus getStreaming() {
    return streaming;
  }

  public void setStreaming(StreamStreamingStatus streaming) {
    this.streaming = streaming;
  }

  public StreamEventsStatus getEvents() {
    return events;
  }

  public void setEvents(StreamEventsStatus events) {
    this.events = events;
  }

  public String getFailure() {
    return failure;
  }

  public void setFailure(String failure) {
    this.failure = failure;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

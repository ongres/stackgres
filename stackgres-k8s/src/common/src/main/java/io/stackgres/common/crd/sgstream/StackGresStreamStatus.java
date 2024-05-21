/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgstream;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.Condition;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresStreamStatus {

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @Valid
  private List<Condition> conditions = new ArrayList<>();

  @Valid
  private StackGresStreamSnapshotStatus snapshot;

  @Valid
  private StackGresStreamStreamingStatus streaming;

  @Valid
  private StackGresStreamEventsStatus events;

  @Valid
  private String failure;

  public List<Condition> getConditions() {
    return conditions;
  }

  public void setConditions(List<Condition> conditions) {
    this.conditions = conditions;
  }

  public StackGresStreamSnapshotStatus getSnapshot() {
    return snapshot;
  }

  public void setSnapshot(StackGresStreamSnapshotStatus snapshot) {
    this.snapshot = snapshot;
  }

  public StackGresStreamStreamingStatus getStreaming() {
    return streaming;
  }

  public void setStreaming(StackGresStreamStreamingStatus streaming) {
    this.streaming = streaming;
  }

  public StackGresStreamEventsStatus getEvents() {
    return events;
  }

  public void setEvents(StackGresStreamEventsStatus events) {
    this.events = events;
  }

  public String getFailure() {
    return failure;
  }

  public void setFailure(String failure) {
    this.failure = failure;
  }

  @Override
  public int hashCode() {
    return Objects.hash(conditions, events, failure, snapshot, streaming);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresStreamStatus)) {
      return false;
    }
    StackGresStreamStatus other = (StackGresStreamStatus) obj;
    return Objects.equals(conditions, other.conditions) && Objects.equals(events, other.events)
        && Objects.equals(failure, other.failure) && Objects.equals(snapshot, other.snapshot)
        && Objects.equals(streaming, other.streaming);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

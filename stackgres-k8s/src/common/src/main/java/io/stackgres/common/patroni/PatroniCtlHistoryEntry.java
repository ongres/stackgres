/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.patroni;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PatroniCtlHistoryEntry {

  @JsonProperty("TL")
  private String timeline;

  @JsonProperty("LSN")
  private String lsn;

  @JsonProperty("Reason")
  private String reason;

  @JsonProperty("Timestamp")
  private String timestamp;

  @JsonProperty("New Leader")
  private String newLeader;

  public String getTimeline() {
    return timeline;
  }

  public void setTimeline(String timeline) {
    this.timeline = timeline;
  }

  public String getLsn() {
    return lsn;
  }

  public void setLsn(String lsn) {
    this.lsn = lsn;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  public String getNewLeader() {
    return newLeader;
  }

  public void setNewLeader(String newLeader) {
    this.newLeader = newLeader;
  }

  @Override
  public int hashCode() {
    return Objects.hash(lsn, newLeader, reason, timeline, timestamp);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PatroniCtlHistoryEntry)) {
      return false;
    }
    PatroniCtlHistoryEntry other = (PatroniCtlHistoryEntry) obj;
    return Objects.equals(lsn, other.lsn) && Objects.equals(newLeader, other.newLeader)
        && Objects.equals(reason, other.reason) && Objects.equals(timeline, other.timeline)
        && Objects.equals(timestamp, other.timestamp);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.patroni;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class PatroniConfig {

  private Integer ttl;

  @JsonProperty("loop_wait")
  private Integer loopWait;

  @JsonProperty("retry_timeout")
  private Integer retryTimeout;

  @JsonProperty("maximum_lag_on_failover")
  private Integer maximumLagOnFailover;

  @JsonProperty("maximum_lag_on_syncnode")
  private Integer maximumLagOnSyncNode;

  @JsonProperty("max_timelines_history")
  private Integer maxTimelinesHistory;

  @JsonProperty("primary_start_timeout")
  private Integer primaryStartTimeout;

  @JsonProperty("master_start_timeout")
  private Integer masterStartTimeout;

  @JsonProperty("primary_stop_timeout")
  private Integer primaryStopTimeout;

  @JsonProperty("synchronous_mode")
  private Boolean synchronousMode;

  @JsonProperty("synchronous_mode_strict")
  private Boolean synchronousModeStrict;

  @JsonProperty("failsafe_mode")
  private Boolean failsafeMode;

  private PostgreSql postgresql;

  @JsonProperty("standby_cluster")
  private StandbyCluster standbyCluster;

  @JsonProperty("check_timeline")
  private Boolean checkTimeline;

  @JsonProperty("synchronous_node_count")
  private Integer synchronousNodeCount;

  @JsonProperty("member_slots_ttl")
  private String memberSlotsTtl;

  private Map<String, Slot> slots;

  @JsonProperty("ignore_slots")
  private List<IgnoredSlot> ignoreSlots;

  public Integer getTtl() {
    return ttl;
  }

  public void setTtl(Integer ttl) {
    this.ttl = ttl;
  }

  public Integer getLoopWait() {
    return loopWait;
  }

  public void setLoopWait(Integer loopWait) {
    this.loopWait = loopWait;
  }

  public Integer getRetryTimeout() {
    return retryTimeout;
  }

  public void setRetryTimeout(Integer retryTimeout) {
    this.retryTimeout = retryTimeout;
  }

  public Integer getMaximumLagOnFailover() {
    return maximumLagOnFailover;
  }

  public void setMaximumLagOnFailover(Integer maximumLagOnFailover) {
    this.maximumLagOnFailover = maximumLagOnFailover;
  }

  public Integer getMaximumLagOnSyncNode() {
    return maximumLagOnSyncNode;
  }

  public void setMaximumLagOnSyncNode(Integer maximumLagOnSyncNode) {
    this.maximumLagOnSyncNode = maximumLagOnSyncNode;
  }

  public Integer getMaxTimelinesHistory() {
    return maxTimelinesHistory;
  }

  public void setMaxTimelinesHistory(Integer maxTimelinesHistory) {
    this.maxTimelinesHistory = maxTimelinesHistory;
  }

  public Integer getPrimaryStartTimeout() {
    return primaryStartTimeout;
  }

  public void setPrimaryStartTimeout(Integer primaryStartTimeout) {
    this.primaryStartTimeout = primaryStartTimeout;
  }

  public Integer getMasterStartTimeout() {
    return masterStartTimeout;
  }

  public void setMasterStartTimeout(Integer masterStartTimeout) {
    this.masterStartTimeout = masterStartTimeout;
  }

  public Integer getPrimaryStopTimeout() {
    return primaryStopTimeout;
  }

  public void setPrimaryStopTimeout(Integer primaryStopTimeout) {
    this.primaryStopTimeout = primaryStopTimeout;
  }

  public Boolean getSynchronousMode() {
    return synchronousMode;
  }

  public void setSynchronousMode(Boolean synchronousMode) {
    this.synchronousMode = synchronousMode;
  }

  public Boolean getSynchronousModeStrict() {
    return synchronousModeStrict;
  }

  public void setSynchronousModeStrict(Boolean synchronousModeStrict) {
    this.synchronousModeStrict = synchronousModeStrict;
  }

  public Boolean getFailsafeMode() {
    return failsafeMode;
  }

  public void setFailsafeMode(Boolean failsafeMode) {
    this.failsafeMode = failsafeMode;
  }

  public PostgreSql getPostgresql() {
    return postgresql;
  }

  public void setPostgresql(PostgreSql postgresql) {
    this.postgresql = postgresql;
  }

  public StandbyCluster getStandbyCluster() {
    return standbyCluster;
  }

  public void setStandbyCluster(StandbyCluster standbyCluster) {
    this.standbyCluster = standbyCluster;
  }

  public Boolean getCheckTimeline() {
    return checkTimeline;
  }

  public void setCheckTimeline(Boolean checkTimeline) {
    this.checkTimeline = checkTimeline;
  }

  public Integer getSynchronousNodeCount() {
    return synchronousNodeCount;
  }

  public void setSynchronousNodeCount(Integer synchronousNodeCount) {
    this.synchronousNodeCount = synchronousNodeCount;
  }

  public String getMemberSlotsTtl() {
    return memberSlotsTtl;
  }

  public void setMemberSlotsTtl(String memberSlotsTtl) {
    this.memberSlotsTtl = memberSlotsTtl;
  }

  public Map<String, Slot> getSlots() {
    return slots;
  }

  public void setSlots(Map<String, Slot> slots) {
    this.slots = slots;
  }

  public List<IgnoredSlot> getIgnoreSlots() {
    return ignoreSlots;
  }

  public void setIgnoreSlots(List<IgnoredSlot> ignoreSlots) {
    this.ignoreSlots = ignoreSlots;
  }

  @Override
  public int hashCode() {
    return Objects.hash(checkTimeline, failsafeMode, ignoreSlots, loopWait, masterStartTimeout,
        maxTimelinesHistory, maximumLagOnFailover, maximumLagOnSyncNode, postgresql,
        primaryStartTimeout, primaryStopTimeout, retryTimeout, slots, standbyCluster,
        synchronousMode, synchronousModeStrict, synchronousNodeCount, ttl);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PatroniConfig)) {
      return false;
    }
    PatroniConfig other = (PatroniConfig) obj;
    return Objects.equals(checkTimeline, other.checkTimeline)
        && Objects.equals(failsafeMode, other.failsafeMode)
        && Objects.equals(ignoreSlots, other.ignoreSlots)
        && Objects.equals(loopWait, other.loopWait)
        && Objects.equals(masterStartTimeout, other.masterStartTimeout)
        && Objects.equals(maxTimelinesHistory, other.maxTimelinesHistory)
        && Objects.equals(maximumLagOnFailover, other.maximumLagOnFailover)
        && Objects.equals(maximumLagOnSyncNode, other.maximumLagOnSyncNode)
        && Objects.equals(postgresql, other.postgresql)
        && Objects.equals(primaryStartTimeout, other.primaryStartTimeout)
        && Objects.equals(primaryStopTimeout, other.primaryStopTimeout)
        && Objects.equals(retryTimeout, other.retryTimeout)
        && Objects.equals(slots, other.slots)
        && Objects.equals(standbyCluster, other.standbyCluster)
        && Objects.equals(synchronousMode, other.synchronousMode)
        && Objects.equals(synchronousModeStrict, other.synchronousModeStrict)
        && Objects.equals(synchronousNodeCount, other.synchronousNodeCount)
        && Objects.equals(ttl, other.ttl);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

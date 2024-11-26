/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.patroni;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.kubernetes.api.model.AnyType;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PatroniMember {

  public static final String LEADER = "Leader";
  public static final String MASTER = "Master";
  public static final String STANDBY_LEADER = "Standby Leader";
  public static final String SYNC_STANDBY = "Sync Standby";
  public static final String REPLICA = "Replica";
  public static final String RUNNING = "running";
  public static final String STOPPED = "stopped";

  @JsonProperty("Cluster")
  private String cluster;

  @JsonProperty("Member")
  private String member;

  @JsonProperty("Host")
  private String host;

  @JsonProperty("Role")
  private String role;

  @JsonProperty("State")
  private String state;

  @JsonProperty("TL")
  private String timeline;

  @JsonProperty("Lag in MB")
  private IntOrString lagInMb;

  @JsonProperty("Group")
  private IntOrString group;

  @JsonProperty("Pending restart")
  private String pendingRestart;

  @JsonProperty("Scheduled restart")
  private String scheduledRestart;

  @JsonProperty("Tags")
  private Map<String, AnyType> tags;

  @JsonIgnore
  public boolean isPrimary() {
    return MemberRole.LEADER == getMemberRole();
  }

  @JsonIgnore
  public boolean isReplica() {
    return MemberRole.REPLICA == getMemberRole();
  }

  @JsonIgnore
  public boolean isRunning() {
    return MemberState.RUNNING == getMemberState();
  }

  @JsonIgnore
  public String getLabelRole(int patroniMajorVersion) {
    if (role == null) {
      return null;
    }
    switch (role) {
      case LEADER: {
        if (patroniMajorVersion < PatroniUtil.PATRONI_VERSION_4) {
          return PatroniUtil.OLD_PRIMARY_ROLE;
        }
        return PatroniUtil.PRIMARY_ROLE;
      }
      case STANDBY_LEADER: return PatroniUtil.STANDBY_LEADER_ROLE;
      case SYNC_STANDBY: return PatroniUtil.SYNC_STANDBY_ROLE;
      case REPLICA: return PatroniUtil.REPLICA_ROLE;
      default: return null;
    }
  }

  @JsonIgnore
  public MemberRole getMemberRole() {
    return MemberRole.fromString(role);
  }

  @JsonIgnore
  public MemberState getMemberState() {
    return MemberState.fromString(state);
  }

  public String getCluster() {
    return cluster;
  }

  public void setCluster(String cluster) {
    this.cluster = cluster;
  }

  public String getMember() {
    return member;
  }

  public void setMember(String member) {
    this.member = member;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getTimeline() {
    return timeline;
  }

  public void setTimeline(String timeline) {
    this.timeline = timeline;
  }

  public IntOrString getLagInMb() {
    return lagInMb;
  }

  public void setLagInMb(IntOrString lagInMb) {
    this.lagInMb = lagInMb;
  }

  public IntOrString getGroup() {
    return group;
  }

  public void setGroup(IntOrString group) {
    this.group = group;
  }

  public String getPendingRestart() {
    return pendingRestart;
  }

  public void setPendingRestart(String pendingRestart) {
    this.pendingRestart = pendingRestart;
  }

  public String getScheduledRestart() {
    return scheduledRestart;
  }

  public void setScheduledRestart(String scheduledRestart) {
    this.scheduledRestart = scheduledRestart;
  }

  public Map<String, AnyType> getTags() {
    return tags;
  }

  public void setTags(Map<String, AnyType> tags) {
    this.tags = tags;
  }

  @Override
  public int hashCode() {
    return Objects.hash(cluster, group, host, lagInMb, member, pendingRestart, role, scheduledRestart, state, tags,
        timeline);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PatroniMember)) {
      return false;
    }
    PatroniMember other = (PatroniMember) obj;
    return Objects.equals(cluster, other.cluster) && Objects.equals(group, other.group)
        && Objects.equals(host, other.host) && Objects.equals(lagInMb, other.lagInMb)
        && Objects.equals(member, other.member) && Objects.equals(pendingRestart, other.pendingRestart)
        && Objects.equals(role, other.role) && Objects.equals(scheduledRestart, other.scheduledRestart)
        && Objects.equals(state, other.state) && Objects.equals(tags, other.tags)
        && Objects.equals(timeline, other.timeline);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

  public enum MemberState {
    RUNNING, STOPPED, STARTING, RESTARTING, FAILED, UNKNOWN;

    static MemberState fromString(String state) {
      if (state == null) {
        return MemberState.UNKNOWN;
      }
      return switch (state) {
        case "running", "streaming", "in archive recovery", "initializing new cluster" -> MemberState.RUNNING;
        case "stopped", "stopping" -> MemberState.STOPPED;
        case "starting", "running custom bootstrap script", "creating replica" -> MemberState.STARTING;
        case "restarting" -> MemberState.RESTARTING;
        case "start failed", "stop failed", "initdb failed", "crashed",
            "custom bootstrap failed" -> MemberState.FAILED;
        default -> state.startsWith("restart failed") ? MemberState.FAILED : MemberState.UNKNOWN;
      };
    }
  }

  public enum MemberRole {
    LEADER, REPLICA, UNKNOWN;
  
    static MemberRole fromString(String role) {
      if (role == null) {
        return MemberRole.UNKNOWN;
      }
      return switch (role) {
        case PatroniMember.LEADER, PatroniMember.MASTER, PatroniMember.STANDBY_LEADER -> MemberRole.LEADER;
        case PatroniMember.REPLICA, PatroniMember.SYNC_STANDBY -> MemberRole.REPLICA;
        default -> MemberRole.UNKNOWN;
      };
    }
  }

}

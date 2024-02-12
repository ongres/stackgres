/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.patroni;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PatroniCtlMember {

  public static final String LEADER = "Leader";
  public static final String STANDBY_LEADER = "Standby Leader";
  public static final String SYNC_STANDBY = "Sync Standby";
  public static final String REPLICA = "Replica";

  @JsonProperty("Cluster")
  private String cluster;

  @JsonProperty("Group")
  private Integer group;

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

  @JsonProperty("Pending restart")
  private String pendingRestart;

  @JsonIgnore
  public boolean isPrimary() {
    return Objects.equals(LEADER, role);
  }

  @JsonIgnore
  public String getLabelRole() {
    if (role == null) {
      return null;
    }
    switch (role) {
      case LEADER: return PatroniUtil.PRIMARY_ROLE;
      case STANDBY_LEADER: return PatroniUtil.STANDBY_LEADER_ROLE;
      case SYNC_STANDBY: return PatroniUtil.SYNC_STANDBY_ROLE;
      case REPLICA: return PatroniUtil.REPLICA_ROLE;
      default: return null;
    }
  }

  public String getCluster() {
    return cluster;
  }

  public void setCluster(String cluster) {
    this.cluster = cluster;
  }

  public Integer getGroup() {
    return group;
  }

  public void setGroup(Integer group) {
    this.group = group;
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

  public String getPendingRestart() {
    return pendingRestart;
  }

  public void setPendingRestart(String pendingRestart) {
    this.pendingRestart = pendingRestart;
  }

  @Override
  public int hashCode() {
    return Objects.hash(cluster, group, host, member, pendingRestart, role, state, timeline);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PatroniCtlMember)) {
      return false;
    }
    PatroniCtlMember other = (PatroniCtlMember) obj;
    return Objects.equals(cluster, other.cluster) && Objects.equals(group, other.group)
        && Objects.equals(host, other.host) && Objects.equals(member, other.member)
        && Objects.equals(pendingRestart, other.pendingRestart) && Objects.equals(role, other.role)
        && Objects.equals(state, other.state) && Objects.equals(timeline, other.timeline);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

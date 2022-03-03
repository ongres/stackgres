/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.patroni;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_EMPTY)
@RegisterForReflection
public class PatroniConfig {

  @JsonProperty("ttl")
  private Integer ttl;

  @JsonProperty("loop_wait")
  private Integer loopWait;

  @JsonProperty("retry_timeout")
  private Integer retryTimeout;

  @JsonProperty("maximum_lag_on_failover")
  private Integer maximumLagOnFailover;

  @JsonProperty("check_timeline")
  private Boolean checkTimeline;

  @JsonProperty("master_start_timeout")
  private Integer masterStartTimeout;

  @JsonProperty("synchronous_mode")
  private Boolean synchronousMode;

  @JsonProperty("synchronous_mode_strict")
  private Boolean synchronousModeStrict;

  @JsonProperty("postgresql")
  private PostgreSql postgresql;

  @JsonProperty("standby_cluster")
  private StandbyCluster standbyCluster;

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

  public Boolean getCheckTimeline() {
    return checkTimeline;
  }

  public void setCheckTimeline(Boolean checkTimeline) {
    this.checkTimeline = checkTimeline;
  }

  public Integer getMasterStartTimeout() {
    return masterStartTimeout;
  }

  public void setMasterStartTimeout(Integer masterStartTimeout) {
    this.masterStartTimeout = masterStartTimeout;
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

  @JsonDeserialize
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(Include.NON_EMPTY)
  @RegisterForReflection
  public static class PostgreSql {

    @JsonProperty("use_slots")
    private Boolean useSlots;

    @JsonProperty("use_pg_rewind")
    private Boolean usePgRewind;

    @JsonProperty("parameters")
    private Map<String, String> parameters;

    @JsonProperty("recovery_conf")
    private Map<String, String> recoveryConf;

    public Boolean getUseSlots() {
      return useSlots;
    }

    public void setUseSlots(Boolean useSlots) {
      this.useSlots = useSlots;
    }

    public Boolean getUsePgRewind() {
      return usePgRewind;
    }

    public void setUsePgRewind(Boolean usePgRewind) {
      this.usePgRewind = usePgRewind;
    }

    public Map<String, String> getParameters() {
      return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
      this.parameters = parameters;
    }

    public Map<String, String> getRecoveryConf() {
      return recoveryConf;
    }

    public void setRecoveryConf(Map<String, String> recoveryConf) {
      this.recoveryConf = recoveryConf;
    }

    @Override
    public int hashCode() {
      return Objects.hash(parameters, recoveryConf, usePgRewind, useSlots);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof PostgreSql)) {
        return false;
      }
      PostgreSql other = (PostgreSql) obj;
      return Objects.equals(parameters, other.parameters)
          && Objects.equals(recoveryConf, other.recoveryConf)
          && Objects.equals(usePgRewind, other.usePgRewind)
          && Objects.equals(useSlots, other.useSlots);
    }

    @Override
    public String toString() {
      return StackGresUtil.toPrettyYaml(this);
    }

  }

  @Override
  public int hashCode() {
    return Objects.hash(checkTimeline, loopWait, masterStartTimeout, maximumLagOnFailover,
        postgresql, retryTimeout, synchronousMode, synchronousModeStrict, ttl);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof PatroniConfig)) {
      return false;
    }
    PatroniConfig other = (PatroniConfig) obj;
    return Objects.equals(checkTimeline, other.checkTimeline)
        && Objects.equals(loopWait, other.loopWait)
        && Objects.equals(masterStartTimeout, other.masterStartTimeout)
        && Objects.equals(maximumLagOnFailover, other.maximumLagOnFailover)
        && Objects.equals(postgresql, other.postgresql)
        && Objects.equals(retryTimeout, other.retryTimeout)
        && Objects.equals(synchronousMode, other.synchronousMode)
        && Objects.equals(synchronousModeStrict, other.synchronousModeStrict)
        && Objects.equals(ttl, other.ttl);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

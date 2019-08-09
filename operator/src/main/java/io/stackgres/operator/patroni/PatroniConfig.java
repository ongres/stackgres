/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_EMPTY)
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

  @JsonInclude(Include.NON_EMPTY)
  public static class PostgreSql {

    @JsonProperty("use_slots")
    private Boolean useSlots;
    @JsonProperty("use_pg_rewind")
    private Boolean usePgRewind;
    @JsonProperty("parameters")
    private Map<String, String> parameters;

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

  }

}

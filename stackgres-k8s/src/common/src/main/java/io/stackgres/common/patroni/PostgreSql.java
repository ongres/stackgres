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
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_EMPTY)
@RegisterForReflection
public class PostgreSql {

  @JsonProperty("use_slots")
  private Boolean useSlots;

  @JsonProperty("use_pg_rewind")
  private Boolean usePgRewind;

  @JsonProperty("pg_hba")
  private List<String> pgHba;

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

  public List<String> getPgHba() {
    return pgHba;
  }

  public void setPgHba(List<String> pgHba) {
    this.pgHba = pgHba;
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
    return Objects.hash(parameters, pgHba, recoveryConf, usePgRewind, useSlots);
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
    return Objects.equals(parameters, other.parameters) && Objects.equals(pgHba, other.pgHba)
        && Objects.equals(recoveryConf, other.recoveryConf)
        && Objects.equals(usePgRewind, other.usePgRewind)
        && Objects.equals(useSlots, other.useSlots);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.math.BigDecimal;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresDbOpsPgbenchStatusStatementLatency {

  private Integer script;

  private String command;

  private BigDecimal latency;

  private String unit;

  public Integer getScript() {
    return script;
  }

  public void setScript(Integer script) {
    this.script = script;
  }

  public String getCommand() {
    return command;
  }

  public void setCommand(String command) {
    this.command = command;
  }

  public BigDecimal getLatency() {
    return latency;
  }

  public void setLatency(BigDecimal latency) {
    this.latency = latency;
  }

  public String getUnit() {
    return unit;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }

  @Override
  public int hashCode() {
    return Objects.hash(command, latency, script, unit);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDbOpsPgbenchStatusStatementLatency)) {
      return false;
    }
    StackGresDbOpsPgbenchStatusStatementLatency other = (StackGresDbOpsPgbenchStatusStatementLatency) obj;
    return Objects.equals(command, other.command) && Objects.equals(latency, other.latency)
        && Objects.equals(script, other.script) && Objects.equals(unit, other.unit);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

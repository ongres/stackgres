/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresDbOpsStatus {

  @JsonProperty("conditions")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @Valid
  private List<Condition> conditions = new ArrayList<>();

  @JsonProperty("opRetries")
  private Integer opRetries;

  @JsonProperty("opStarted")
  private String opStarted;

  @JsonProperty("benchmark")
  private StackGresDbOpsBenchmarkStatus benchmark;

  @JsonProperty("majorVersionUpgrade")
  private StackGresDbOpsMajorVersionUpgradeStatus majorVersionUpgrade;

  @JsonProperty("restart")
  private StackGresDbOpsRestartStatus restart;

  @JsonProperty("minorVersionUpgrade")
  private StackGresDbOpsMinorVersionUpgradeStatus minorVersionUpgrade;

  @JsonProperty("securityUpgrade")
  private StackGresDbOpsSecurityUpgradeStatus securityUpgrade;

  @ReferencedField("opStarted")
  interface OpStarted extends FieldReference { }

  @JsonIgnore
  @AssertTrue(message = "opStarted must be in ISO 8601 date format: `YYYY-MM-DDThh:mm:ss.ddZ`.",
      payload = OpStarted.class)
  public boolean isOpStartedValid() {
    try {
      if (opStarted != null) {
        Instant.parse(opStarted);
      }
      return true;
    } catch (DateTimeParseException ex) {
      return false;
    }
  }

  public List<Condition> getConditions() {
    return conditions;
  }

  public void setConditions(List<Condition> conditions) {
    this.conditions = conditions;
  }

  public Integer getOpRetries() {
    return opRetries;
  }

  public void setOpRetries(Integer opRetries) {
    this.opRetries = opRetries;
  }

  public String getOpStarted() {
    return opStarted;
  }

  public void setOpStarted(String opStarted) {
    this.opStarted = opStarted;
  }

  public StackGresDbOpsBenchmarkStatus getBenchmark() {
    return benchmark;
  }

  public void setBenchmark(StackGresDbOpsBenchmarkStatus benchmark) {
    this.benchmark = benchmark;
  }

  public StackGresDbOpsMajorVersionUpgradeStatus getMajorVersionUpgrade() {
    return majorVersionUpgrade;
  }

  public void setMajorVersionUpgrade(StackGresDbOpsMajorVersionUpgradeStatus majorVersionUpgrade) {
    this.majorVersionUpgrade = majorVersionUpgrade;
  }

  public StackGresDbOpsRestartStatus getRestart() {
    return restart;
  }

  public void setRestart(StackGresDbOpsRestartStatus restart) {
    this.restart = restart;
  }

  public StackGresDbOpsMinorVersionUpgradeStatus getMinorVersionUpgrade() {
    return minorVersionUpgrade;
  }

  public void setMinorVersionUpgrade(StackGresDbOpsMinorVersionUpgradeStatus minorVersionUpgrade) {
    this.minorVersionUpgrade = minorVersionUpgrade;
  }

  public StackGresDbOpsSecurityUpgradeStatus getSecurityUpgrade() {
    return securityUpgrade;
  }

  public void setSecurityUpgrade(StackGresDbOpsSecurityUpgradeStatus securityUpgrade) {
    this.securityUpgrade = securityUpgrade;
  }

  @Override
  public int hashCode() {
    return Objects.hash(benchmark, conditions, majorVersionUpgrade, minorVersionUpgrade, opRetries,
        opStarted, restart, securityUpgrade);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDbOpsStatus)) {
      return false;
    }
    StackGresDbOpsStatus other = (StackGresDbOpsStatus) obj;
    return Objects.equals(benchmark, other.benchmark)
        && Objects.equals(conditions, other.conditions)
        && Objects.equals(majorVersionUpgrade, other.majorVersionUpgrade)
        && Objects.equals(minorVersionUpgrade, other.minorVersionUpgrade)
        && Objects.equals(opRetries, other.opRetries) && Objects.equals(opStarted, other.opStarted)
        && Objects.equals(restart, other.restart)
        && Objects.equals(securityUpgrade, other.securityUpgrade);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

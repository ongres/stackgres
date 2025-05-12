/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardeddbops;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.stackgres.common.validation.ValidEnum;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresShardedDbOpsSpec {

  @NotEmpty(message = "sgShardedCluster must be provided")
  private String sgShardedCluster;

  private StackGresShardedDbOpsSpecScheduling scheduling;

  @ValidEnum(enumClass = ShardedDbOpsOperationAllowed.class, allowNulls = false,
      message = "op must be one of resharding, restart "
          + " or securityUpgrade")
  private String op;

  private String runAt;

  private String timeout;

  @Min(value = 0, message = "maxRetries must be greather or equals to 0.")
  private Integer maxRetries;

  @Valid
  private StackGresShardedDbOpsResharding resharding;

  @Valid
  private StackGresShardedDbOpsMajorVersionUpgrade majorVersionUpgrade;

  @Valid
  private StackGresShardedDbOpsRestart restart;

  @Valid
  private StackGresShardedDbOpsMinorVersionUpgrade minorVersionUpgrade;

  @Valid
  private StackGresShardedDbOpsSecurityUpgrade securityUpgrade;

  @ReferencedField("op")
  interface Op extends FieldReference {
  }

  @ReferencedField("runAt")
  interface RunAt extends FieldReference {
  }

  @ReferencedField("timeout")
  interface Timeout extends FieldReference {
  }

  @ReferencedField("minorVersionUpgrade")
  interface MinorVersionUpgrade extends FieldReference {
  }

  @ReferencedField("majorVersionUpgrade")
  interface MajorVersionUpgrade extends FieldReference {
  }

  @JsonIgnore
  @AssertTrue(message = "op must match corresponding section.",
      payload = Op.class)
  public boolean isOpMatchSection() {
    if (op == null) {
      return true;
    }
    final ShardedDbOpsOperation op;
    try {
      op = ShardedDbOpsOperation.fromString(this.op);
    } catch (IllegalArgumentException ex) {
      return true;
    }
    switch (op) {
      case RESHARDING:
        return restart == null
            && majorVersionUpgrade == null && minorVersionUpgrade == null
            && securityUpgrade == null;
      case RESTART:
        return resharding == null
            && majorVersionUpgrade == null && minorVersionUpgrade == null
            && securityUpgrade == null;
      case SECURITY_UPGRADE:
        return resharding == null && restart == null
            && majorVersionUpgrade == null && minorVersionUpgrade == null;
      case MAJOR_VERSION_UPGRADE:
        return resharding == null && restart == null
            && securityUpgrade == null && minorVersionUpgrade == null;
      case MINOR_VERSION_UPGRADE:
        return resharding == null && restart == null
            && majorVersionUpgrade == null && securityUpgrade == null;
      default:
        break;
    }
    return true;
  }

  @JsonIgnore
  @AssertTrue(message = "runAt must be in ISO 8601 date format: `YYYY-MM-DDThh:mm:ss.ddZ`.",
      payload = RunAt.class)
  public boolean isRunAtValid() {
    try {
      if (runAt != null) {
        Instant.parse(runAt);
      }
      return true;
    } catch (DateTimeParseException ex) {
      return false;
    }
  }

  @JsonIgnore
  @AssertTrue(message = "timeout must be positive and in ISO 8601 duration format:"
      + " `PnDTnHnMn.nS`.",
      payload = Timeout.class)
  public boolean isTimeoutValid() {
    try {
      if (timeout != null) {
        return !Duration.parse(timeout).isNegative();
      }
      return true;
    } catch (DateTimeParseException ex) {
      return false;
    }
  }

  @JsonIgnore
  @AssertTrue(message = "minorVersionUpgrade section must be provided.",
      payload = MinorVersionUpgrade.class)
  public boolean isMinorVersionUpgradeSectionProvided() {
    if (Objects.equals(op, ShardedDbOpsOperation.MINOR_VERSION_UPGRADE.toString())) {
      return minorVersionUpgrade != null;
    }
    return true;
  }

  @JsonIgnore
  @AssertTrue(message = "majorVersionUpgrade section must be provided.",
      payload = MajorVersionUpgrade.class)
  public boolean isMajorVersionUpgradeSectionProvided() {
    if (Objects.equals(op, ShardedDbOpsOperation.MAJOR_VERSION_UPGRADE.toString())) {
      return majorVersionUpgrade != null;
    }
    return true;
  }

  @JsonIgnore
  public boolean isOpResharding() {
    return Objects.equals(op, ShardedDbOpsOperation.RESHARDING.toString());
  }

  @JsonIgnore
  public boolean isOpMajorVersionUpgrade() {
    return Objects.equals(op, ShardedDbOpsOperation.MAJOR_VERSION_UPGRADE.toString());
  }

  @JsonIgnore
  public boolean isOpRestart() {
    return Objects.equals(op, ShardedDbOpsOperation.RESTART.toString());
  }

  @JsonIgnore
  public boolean isOpMinorVersionUpgrade() {
    return Objects.equals(op, ShardedDbOpsOperation.MINOR_VERSION_UPGRADE.toString());
  }

  @JsonIgnore
  public boolean isOpSecurityUpgrade() {
    return Objects.equals(op, ShardedDbOpsOperation.SECURITY_UPGRADE.toString());
  }

  public String getSgShardedCluster() {
    return sgShardedCluster;
  }

  public void setSgShardedCluster(String sgShardedCluster) {
    this.sgShardedCluster = sgShardedCluster;
  }

  public StackGresShardedDbOpsSpecScheduling getScheduling() {
    return scheduling;
  }

  public void setScheduling(StackGresShardedDbOpsSpecScheduling scheduling) {
    this.scheduling = scheduling;
  }

  public String getOp() {
    return op;
  }

  public void setOp(String op) {
    this.op = op;
  }

  public String getRunAt() {
    return runAt;
  }

  public void setRunAt(String runAt) {
    this.runAt = runAt;
  }

  public String getTimeout() {
    return timeout;
  }

  public void setTimeout(String timeout) {
    this.timeout = timeout;
  }

  public Integer getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(Integer maxRetries) {
    this.maxRetries = maxRetries;
  }

  public StackGresShardedDbOpsResharding getResharding() {
    return resharding;
  }

  public void setResharding(StackGresShardedDbOpsResharding resharding) {
    this.resharding = resharding;
  }

  public StackGresShardedDbOpsMajorVersionUpgrade getMajorVersionUpgrade() {
    return majorVersionUpgrade;
  }

  public void setMajorVersionUpgrade(StackGresShardedDbOpsMajorVersionUpgrade majorVersionUpgrade) {
    this.majorVersionUpgrade = majorVersionUpgrade;
  }

  public StackGresShardedDbOpsRestart getRestart() {
    return restart;
  }

  public void setRestart(StackGresShardedDbOpsRestart restart) {
    this.restart = restart;
  }

  public StackGresShardedDbOpsMinorVersionUpgrade getMinorVersionUpgrade() {
    return minorVersionUpgrade;
  }

  public void setMinorVersionUpgrade(StackGresShardedDbOpsMinorVersionUpgrade minorVersionUpgrade) {
    this.minorVersionUpgrade = minorVersionUpgrade;
  }

  public StackGresShardedDbOpsSecurityUpgrade getSecurityUpgrade() {
    return securityUpgrade;
  }

  public void setSecurityUpgrade(StackGresShardedDbOpsSecurityUpgrade securityUpgrade) {
    this.securityUpgrade = securityUpgrade;
  }

  @Override
  public int hashCode() {
    return Objects.hash(majorVersionUpgrade, maxRetries, minorVersionUpgrade, op, resharding,
        restart, runAt, scheduling, securityUpgrade, sgShardedCluster, timeout);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresShardedDbOpsSpec)) {
      return false;
    }
    StackGresShardedDbOpsSpec other = (StackGresShardedDbOpsSpec) obj;
    return Objects.equals(majorVersionUpgrade, other.majorVersionUpgrade)
        && Objects.equals(maxRetries, other.maxRetries)
        && Objects.equals(minorVersionUpgrade, other.minorVersionUpgrade)
        && Objects.equals(op, other.op)
        && Objects.equals(resharding, other.resharding)
        && Objects.equals(restart, other.restart)
        && Objects.equals(runAt, other.runAt)
        && Objects.equals(scheduling, other.scheduling)
        && Objects.equals(securityUpgrade, other.securityUpgrade)
        && Objects.equals(sgShardedCluster, other.sgShardedCluster)
        && Objects.equals(timeout, other.timeout);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

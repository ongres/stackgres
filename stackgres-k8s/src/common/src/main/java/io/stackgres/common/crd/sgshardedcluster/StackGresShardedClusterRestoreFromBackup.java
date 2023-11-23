/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresShardedClusterRestoreFromBackup {

  @NotNull
  private String name;

  private Boolean targetInclusive;

  @Valid
  private StackGresShardedClusterRestorePitr pointInTimeRecovery;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Boolean getTargetInclusive() {
    return targetInclusive;
  }

  public void setTargetInclusive(Boolean targetInclusive) {
    this.targetInclusive = targetInclusive;
  }

  public StackGresShardedClusterRestorePitr getPointInTimeRecovery() {
    return pointInTimeRecovery;
  }

  public void setPointInTimeRecovery(StackGresShardedClusterRestorePitr pointInTimeRecovery) {
    this.pointInTimeRecovery = pointInTimeRecovery;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresShardedClusterRestoreFromBackup)) {
      return false;
    }
    StackGresShardedClusterRestoreFromBackup other = (StackGresShardedClusterRestoreFromBackup) obj;
    return Objects.equals(name, other.name)
        && Objects.equals(pointInTimeRecovery, other.pointInTimeRecovery)
        && Objects.equals(targetInclusive, other.targetInclusive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, pointInTimeRecovery, targetInclusive);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

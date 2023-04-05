/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterRestoreFromBackup {

  private String uid;

  private String name;

  private String target;

  private String targetTimeline;

  private Boolean targetInclusive;

  private String targetName;

  private String targetXid;

  private String targetLsn;

  @JsonProperty("pointInTimeRecovery")
  @Valid
  private StackGresClusterRestorePitr pointInTimeRecovery;

  @ReferencedField("name")
  interface Name extends FieldReference { }

  @ReferencedField("targetName")
  interface TargetName extends FieldReference { }

  @ReferencedField("targetXid")
  interface TargetXid extends FieldReference { }

  @ReferencedField("targetLsn")
  interface TargetLsn extends FieldReference { }

  @ReferencedField("pointInTimeRecovery")
  interface PointInTimeRecovery extends FieldReference { }

  @JsonIgnore
  @AssertTrue(message = "name cannot be null",
      payload = { Name.class })
  public boolean isNameNotNullOrUidNotNull() {
    return (name != null && uid == null) // NOPMD
        || (name == null && uid != null); // NOPMD
  }

  @JsonIgnore
  @AssertTrue(message = "targetName, targetLsn, targetXid pointInTimeRecovery"
      + " are mutually exclusive",
      payload = { TargetName.class, TargetXid.class,
          TargetLsn.class, PointInTimeRecovery.class })
  public boolean isJustOneTarget() {
    return (targetName == null && targetXid == null
        && targetLsn == null && pointInTimeRecovery == null) // NOPMD
        || (targetName != null && targetXid == null
        && targetLsn == null && pointInTimeRecovery == null) // NOPMD
        || (targetName == null && targetXid != null
        && targetLsn == null && pointInTimeRecovery == null) // NOPMD
        || (targetName == null && targetXid == null
        && targetLsn != null && pointInTimeRecovery == null) // NOPMD
        || (targetName == null && targetXid == null
        && targetLsn == null && pointInTimeRecovery != null); // NOPMD
  }

  public String getUid() {
    return uid;
  }

  public void setUid(String uid) {
    this.uid = uid;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public String getTargetTimeline() {
    return targetTimeline;
  }

  public void setTargetTimeline(String targetTimeline) {
    this.targetTimeline = targetTimeline;
  }

  public Boolean getTargetInclusive() {
    return targetInclusive;
  }

  public void setTargetInclusive(Boolean targetInclusive) {
    this.targetInclusive = targetInclusive;
  }

  public String getTargetName() {
    return targetName;
  }

  public void setTargetName(String targetName) {
    this.targetName = targetName;
  }

  public String getTargetXid() {
    return targetXid;
  }

  public void setTargetXid(String targetXid) {
    this.targetXid = targetXid;
  }

  public String getTargetLsn() {
    return targetLsn;
  }

  public void setTargetLsn(String targetLsn) {
    this.targetLsn = targetLsn;
  }

  public StackGresClusterRestorePitr getPointInTimeRecovery() {
    return pointInTimeRecovery;
  }

  public void setPointInTimeRecovery(StackGresClusterRestorePitr pointInTimeRecovery) {
    this.pointInTimeRecovery = pointInTimeRecovery;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterRestoreFromBackup)) {
      return false;
    }
    StackGresClusterRestoreFromBackup other = (StackGresClusterRestoreFromBackup) obj;
    return Objects.equals(name, other.name)
        && Objects.equals(pointInTimeRecovery, other.pointInTimeRecovery)
        && Objects.equals(target, other.target)
        && Objects.equals(targetInclusive, other.targetInclusive)
        && Objects.equals(targetLsn, other.targetLsn)
        && Objects.equals(targetName, other.targetName)
        && Objects.equals(targetTimeline, other.targetTimeline)
        && Objects.equals(targetXid, other.targetXid)
        && Objects.equals(uid, other.uid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, pointInTimeRecovery, target, targetInclusive, targetLsn, targetName,
        targetTimeline, targetXid, uid);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgstream;

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

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresStreamTarget {

  @ValidEnum(enumClass = StreamTargetType.class, allowNulls = false,
      message = "type must be one of CloudEvent or SGCluster")
  private String type;

  @Valid
  private StackGresStreamTargetCloudEvent cloudEvent;

  @Valid
  private StackGresStreamTargetSgCluster sgCluster;

  @ReferencedField("type")
  interface Type extends FieldReference {
  }

  @ReferencedField("cloudEvent")
  interface CloudEvent extends FieldReference {
  }

  @ReferencedField("sgCluster")
  interface SgCluster extends FieldReference {
  }

  @JsonIgnore
  @AssertTrue(message = "type must match corresponding section.",
      payload = Type.class)
  public boolean isTypeMatchSection() {
    if (type != null) {
      switch (type) {
        case "SGCluster":
          return cloudEvent == null;
        case "CloudEvent":
          return sgCluster == null;
        default:
          break;
      }
    }
    return true;
  }

  @JsonIgnore
  @AssertTrue(message = "sgCluster must not be null",
      payload = SgCluster.class)
  public boolean isSgClusterPresent() {
    return !Objects.equals(type, "SGCluster") || sgCluster != null;
  }

  @JsonIgnore
  @AssertTrue(message = "cloudEvent must not be null",
      payload = CloudEvent.class)
  public boolean isCloudEventPresent() {
    return !Objects.equals(type, "CloudEvent") || cloudEvent != null;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public StackGresStreamTargetCloudEvent getCloudEvent() {
    return cloudEvent;
  }

  public void setCloudEvent(StackGresStreamTargetCloudEvent cloudEvent) {
    this.cloudEvent = cloudEvent;
  }

  public StackGresStreamTargetSgCluster getSgCluster() {
    return sgCluster;
  }

  public void setSgCluster(StackGresStreamTargetSgCluster sgCluster) {
    this.sgCluster = sgCluster;
  }

  @Override
  public int hashCode() {
    return Objects.hash(cloudEvent, sgCluster, type);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresStreamTarget)) {
      return false;
    }
    StackGresStreamTarget other = (StackGresStreamTarget) obj;
    return Objects.equals(cloudEvent, other.cloudEvent)
        && Objects.equals(sgCluster, other.sgCluster) && Objects.equals(type, other.type);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

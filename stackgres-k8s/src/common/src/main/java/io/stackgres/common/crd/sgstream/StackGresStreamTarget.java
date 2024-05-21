/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgstream;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.ValidEnum;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresStreamTarget {

  @ValidEnum(enumClass = StreamTargetType.class, allowNulls = false,
      message = "type must be CloudEvent")
  private String type;

  @Valid
  private StackGresStreamTargetCloudEvent cloudEvent;

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

  @Override
  public int hashCode() {
    return Objects.hash(cloudEvent, type);
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
    return Objects.equals(cloudEvent, other.cloudEvent) && Objects.equals(type, other.type);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

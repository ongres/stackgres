/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

import java.util.List;
import java.util.Objects;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_INTERFACE",
    justification = "Intentional name shadowing")
public class Condition implements io.stackgres.operatorframework.resource.Condition {

  @JsonProperty("lastTransitionTime")
  private String lastTransitionTime;

  @JsonProperty("message")
  private String message;

  @JsonProperty("reason")
  private String reason;

  @JsonProperty("status")
  @NotBlank(message = "The condition status is required")
  private String status;

  @JsonProperty("type")
  @NotBlank(message = "The condition type is required")
  private String type;

  /**
   * Constructor of the required fields.
   *
   * @param type of the condition
   * @param status of the condition, can be True, False, Unknown
   * @param reason of the condition
   */
  public Condition(String type, String status, String reason) {
    this.type = type;
    this.status = status;
    this.reason = reason;
  }

  public Condition() {
  }

  public static void setTransitionTimes(List<? extends Condition> conditions) {
    io.stackgres.operatorframework.resource.Condition.setTransitionTimes(conditions);
  }

  @Override
  public String getLastTransitionTime() {
    return lastTransitionTime;
  }

  @Override
  public void setLastTransitionTime(String lastTransitionTime) {
    this.lastTransitionTime = lastTransitionTime;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public String getReason() {
    return reason;
  }

  @Override
  public void setReason(String reason) {
    this.reason = reason;
  }

  @Override
  public String getStatus() {
    return status;
  }

  @Override
  public void setStatus(String status) {
    this.status = status;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public void setType(String type) {
    this.type = type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(lastTransitionTime, message, reason, status, type);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Condition)) {
      return false;
    }
    Condition other = (Condition) obj;
    return Objects.equals(lastTransitionTime, other.lastTransitionTime)
        && Objects.equals(message, other.message) && Objects.equals(reason, other.reason)
        && Objects.equals(status, other.status) && Objects.equals(type, other.type);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

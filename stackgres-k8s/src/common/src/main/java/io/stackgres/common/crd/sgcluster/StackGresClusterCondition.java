/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.operatorframework.resource.Condition;
import io.sundr.builder.annotations.Buildable;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresClusterCondition implements Condition, KubernetesResource {

  private static final long serialVersionUID = -175006392037845752L;

  @JsonProperty("lastTransitionTime")
  private String lastTransitionTime;

  @JsonProperty("message")
  private String message;

  @JsonProperty("reason")
  @NotBlank(message = "The condition reason is required")
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
  public StackGresClusterCondition(String type, String status, String reason) {
    this.type = type;
    this.status = status;
    this.reason = reason;
  }

  public StackGresClusterCondition() {
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
    if (!(obj instanceof StackGresClusterCondition)) {
      return false;
    }
    StackGresClusterCondition other = (StackGresClusterCondition) obj;
    return Objects.equals(lastTransitionTime, other.lastTransitionTime)
        && Objects.equals(message, other.message) && Objects.equals(reason, other.reason)
        && Objects.equals(status, other.status) && Objects.equals(type, other.type);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

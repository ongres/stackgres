/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.util.Objects;

import javax.validation.constraints.AssertTrue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresDbOpsRestart implements KubernetesResource {

  private static final long serialVersionUID = 1L;

  @JsonProperty("method")
  private String method;

  @JsonProperty("restartPrimaryFirst")
  private Boolean restartPrimaryFirst;

  @ReferencedField("method")
  interface Method extends FieldReference { }

  @JsonIgnore
  @AssertTrue(message = "method must be InPlace or ReducedImpact",
      payload = Method.class)
  public boolean isMethodValid() {
    return method == null
        || ImmutableList.of("InPlace", "ReducedImpact").contains(method);
  }

  @JsonIgnore
  public boolean isMethodReducedImpact() {
    return Objects.equals(method, "ReducedImpact");
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public Boolean getRestartPrimaryFirst() {
    return restartPrimaryFirst;
  }

  public void setRestartPrimaryFirst(Boolean restartPrimaryFirst) {
    this.restartPrimaryFirst = restartPrimaryFirst;
  }

  @Override
  public int hashCode() {
    return Objects.hash(method, restartPrimaryFirst);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDbOpsRestart)) {
      return false;
    }
    StackGresDbOpsRestart other = (StackGresDbOpsRestart) obj;
    return Objects.equals(method, other.method)
        && Objects.equals(restartPrimaryFirst, other.restartPrimaryFirst);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

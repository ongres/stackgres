/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.ValidEnum;
import io.sundr.builder.annotations.Buildable;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresDbOpsRestart implements KubernetesResource {

  private static final long serialVersionUID = 1L;

  @JsonProperty("method")
  @ValidEnum(enumClass = DbOpsMethodType.class, allowNulls = true,
      message = "method must be InPlace or ReducedImpact")
  private String method;

  @JsonProperty("restartPrimaryFirst")
  private Boolean restartPrimaryFirst;

  @JsonProperty("onlyPendingRestart")
  private Boolean onlyPendingRestart;

  @JsonIgnore
  public boolean isMethodReducedImpact() {
    return Objects.equals(method, DbOpsMethodType.REDUCED_IMPACT.toString());
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

  public Boolean getOnlyPendingRestart() {
    return onlyPendingRestart;
  }

  public void setOnlyPendingRestart(Boolean onlyPendingRestart) {
    this.onlyPendingRestart = onlyPendingRestart;
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

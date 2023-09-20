/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.ValidEnum;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresDbOpsRestart {

  @ValidEnum(enumClass = DbOpsMethodType.class, allowNulls = true,
      message = "method must be InPlace or ReducedImpact")
  private String method;

  private Boolean restartPrimaryFirst;

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

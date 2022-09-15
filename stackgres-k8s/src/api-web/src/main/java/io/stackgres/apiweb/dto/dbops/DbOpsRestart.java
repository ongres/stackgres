/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.dbops;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DbOpsRestart {

  @JsonProperty("method")
  private String method;

  @JsonProperty("restartPrimaryFirst")
  private Boolean restartPrimaryFirst;

  @JsonProperty("onlyPendingRestart")
  private Boolean onlyPendingRestart;

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
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

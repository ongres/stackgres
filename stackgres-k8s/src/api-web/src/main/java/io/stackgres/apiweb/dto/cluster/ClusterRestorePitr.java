/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ClusterRestorePitr {

  @JsonProperty("restoreToTimestamp")
  private String restoreToTimestamp;

  public String getRestoreToTimestamp() {
    return restoreToTimestamp;
  }

  public void setRestoreToTimestamp(String restoreToTimestamp) {
    this.restoreToTimestamp = restoreToTimestamp;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

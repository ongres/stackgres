/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.stream;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class StreamTarget {

  private String type;

  private StreamTargetCloudEvent cloudEvent;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public StreamTargetCloudEvent getCloudEvent() {
    return cloudEvent;
  }

  public void setCloudEvent(StreamTargetCloudEvent cloudEvent) {
    this.cloudEvent = cloudEvent;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

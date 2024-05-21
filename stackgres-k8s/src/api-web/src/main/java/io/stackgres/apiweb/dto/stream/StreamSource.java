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
public class StreamSource {

  private String type;

  private StreamSourceSgCluster sgCluster;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public StreamSourceSgCluster getSgCluster() {
    return sgCluster;
  }

  public void setSgCluster(StreamSourceSgCluster sgCluster) {
    this.sgCluster = sgCluster;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

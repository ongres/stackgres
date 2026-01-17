/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterReplicateFromExternal {

  private String host;

  private Integer port;

  private ClusterReplicateFromCustomRestoreMethod customRestoreMethod;

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public ClusterReplicateFromCustomRestoreMethod getCustomRestoreMethod() {
    return customRestoreMethod;
  }

  public void setCustomRestoreMethod(ClusterReplicateFromCustomRestoreMethod customRestoreMethod) {
    this.customRestoreMethod = customRestoreMethod;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

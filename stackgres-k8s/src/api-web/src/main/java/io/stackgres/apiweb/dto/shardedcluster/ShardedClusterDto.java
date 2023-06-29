/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedcluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ShardedClusterDto extends ResourceDto {

  @JsonProperty("spec")
  private ShardedClusterSpec spec;

  @JsonProperty("status")
  private ShardedClusterStatus status;

  @JsonProperty("grafanaEmbedded")
  private boolean grafanaEmbedded;

  @JsonProperty("info")
  private ShardedClusterInfo info;

  public ShardedClusterSpec getSpec() {
    return spec;
  }

  public void setSpec(ShardedClusterSpec spec) {
    this.spec = spec;
  }

  public ShardedClusterStatus getStatus() {
    return status;
  }

  public void setStatus(ShardedClusterStatus status) {
    this.status = status;
  }

  public boolean isGrafanaEmbedded() {
    return grafanaEmbedded;
  }

  public void setGrafanaEmbedded(boolean grafanaEmbedded) {
    this.grafanaEmbedded = grafanaEmbedded;
  }

  public ShardedClusterInfo getInfo() {
    return info;
  }

  public void setInfo(ShardedClusterInfo info) {
    this.info = info;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

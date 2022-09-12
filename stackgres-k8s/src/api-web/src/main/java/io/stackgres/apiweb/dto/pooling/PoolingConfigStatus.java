/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.pooling;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class PoolingConfigStatus {

  @JsonProperty("clusters")
  private List<String> clusters;

  @JsonProperty("pgBouncer")
  private PoolingConfigPgBouncerStatus pgBouncer;

  public List<String> getClusters() {
    return clusters;
  }

  public void setClusters(List<String> clusters) {
    this.clusters = clusters;
  }

  public PoolingConfigPgBouncerStatus getPgBouncer() {
    return pgBouncer;
  }

  public void setPgBouncer(PoolingConfigPgBouncerStatus pgBouncer) {
    this.pgBouncer = pgBouncer;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

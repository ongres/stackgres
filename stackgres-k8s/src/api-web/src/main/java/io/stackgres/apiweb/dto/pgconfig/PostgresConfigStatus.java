/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.pgconfig;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class PostgresConfigStatus {

  @JsonProperty("clusters")
  private List<String> clusters;

  @JsonProperty("postgresql.conf")
  private List<PostgresqlConfParameter> postgresqlConf;

  @JsonProperty("defaultParameters")
  private Map<String, String> defaultParameters;

  public List<String> getClusters() {
    return clusters;
  }

  public void setClusters(List<String> clusters) {
    this.clusters = clusters;
  }

  public List<PostgresqlConfParameter> getPostgresqlConf() {
    return postgresqlConf;
  }

  public void setPostgresqlConf(List<PostgresqlConfParameter> postgresqlConf) {
    this.postgresqlConf = postgresqlConf;
  }

  public Map<String, String> getDefaultParameters() {
    return defaultParameters;
  }

  public void setDefaultParameters(Map<String, String> defaultParameters) {
    this.defaultParameters = defaultParameters;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

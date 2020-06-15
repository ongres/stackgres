/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.pgconfig;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class PostgresConfigStatus {

  @JsonProperty("clusters")
  @NotNull(message = "clusters is required")
  private List<String> clusters;

  @JsonProperty("postgresql.conf")
  @NotNull(message = "postgresql.conf is required")
  @Valid
  private List<PostgresqlConfParameter> postgresqlConf;

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

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("clusters", clusters)
        .add("postgresql.conf", postgresqlConf)
        .toString();
  }

}

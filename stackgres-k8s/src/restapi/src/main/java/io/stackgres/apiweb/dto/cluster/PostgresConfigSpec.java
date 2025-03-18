/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class PostgresConfigSpec {

  @JsonProperty("postgresql.conf")
  private Map<String, String> postgresqlConf;

  public Map<String, String> getPostgresqlConf() {
    return postgresqlConf;
  }

  public void setPostgresqlConf(Map<String, String> postgresqlConf) {
    this.postgresqlConf = postgresqlConf;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

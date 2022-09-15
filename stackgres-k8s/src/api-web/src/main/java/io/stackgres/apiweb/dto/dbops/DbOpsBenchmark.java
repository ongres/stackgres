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
public class DbOpsBenchmark {

  @JsonProperty("type")
  private String type;

  @JsonProperty("pgbench")
  private DbOpsPgbench pgbench;

  @JsonProperty("connectionType")
  private String connectionType;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public DbOpsPgbench getPgbench() {
    return pgbench;
  }

  public void setPgbench(DbOpsPgbench pgbench) {
    this.pgbench = pgbench;
  }

  public String getConnectionType() {
    return connectionType;
  }

  public void setConnectionType(String connectionType) {
    this.connectionType = connectionType;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

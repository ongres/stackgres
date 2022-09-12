/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.dbops;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DbOpsVacuum extends DbOpsVacuumConfig {

  @JsonProperty("databases")
  private List<DbOpsVacuumDatabase> databases;

  public List<DbOpsVacuumDatabase> getDatabases() {
    return databases;
  }

  public void setDatabases(List<DbOpsVacuumDatabase> databases) {
    this.databases = databases;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

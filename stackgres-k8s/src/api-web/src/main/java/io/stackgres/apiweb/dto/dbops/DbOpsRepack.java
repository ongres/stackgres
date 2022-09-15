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
public class DbOpsRepack extends DbOpsRepackConfig {

  @JsonProperty("databases")
  private List<DbOpsRepackDatabase> databases;

  public List<DbOpsRepackDatabase> getDatabases() {
    return databases;
  }

  public void setDatabases(List<DbOpsRepackDatabase> databases) {
    this.databases = databases;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

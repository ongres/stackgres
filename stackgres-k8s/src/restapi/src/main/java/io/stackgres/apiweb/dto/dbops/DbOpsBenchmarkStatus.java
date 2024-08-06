/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.dbops;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DbOpsBenchmarkStatus {

  private DbOpsPgbenchStatus pgbench;

  public DbOpsPgbenchStatus getPgbench() {
    return pgbench;
  }

  public void setPgbench(DbOpsPgbenchStatus pgbench) {
    this.pgbench = pgbench;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

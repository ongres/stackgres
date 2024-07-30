/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.dbops;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DbOpsSamplingStatus {

  private List<DbOpsSamplingStatusTopQuery> topQueries;

  private List<DbOpsSamplingStatusQuery> queries;

  public List<DbOpsSamplingStatusTopQuery> getTopQueries() {
    return topQueries;
  }

  public void setTopQueries(List<DbOpsSamplingStatusTopQuery> topQueries) {
    this.topQueries = topQueries;
  }

  public List<DbOpsSamplingStatusQuery> getQueries() {
    return queries;
  }

  public void setQueries(List<DbOpsSamplingStatusQuery> queries) {
    this.queries = queries;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

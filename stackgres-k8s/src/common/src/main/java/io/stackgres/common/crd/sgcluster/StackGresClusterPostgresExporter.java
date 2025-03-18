/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StackGresClusterPostgresExporter {

  private StackGresClusterPostgresExporterQueries queries;

  public StackGresClusterPostgresExporterQueries getQueries() {
    return queries;
  }

  public void setQueries(StackGresClusterPostgresExporterQueries queries) {
    this.queries = queries;
  }

  @Override
  public int hashCode() {
    return Objects.hash(queries);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterPostgresExporter)) {
      return false;
    }
    StackGresClusterPostgresExporter other = (StackGresClusterPostgresExporter) obj;
    return Objects.equals(queries, other.queries);
  }

  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

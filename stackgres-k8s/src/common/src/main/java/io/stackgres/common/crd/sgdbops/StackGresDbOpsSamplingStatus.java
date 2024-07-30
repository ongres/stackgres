/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresDbOpsSamplingStatus {

  private List<StackGresDbOpsSamplingStatusTopQuery> topQueries;

  private List<StackGresDbOpsSamplingStatusQuery> queries;

  public List<StackGresDbOpsSamplingStatusTopQuery> getTopQueries() {
    return topQueries;
  }

  public void setTopQueries(List<StackGresDbOpsSamplingStatusTopQuery> topQueries) {
    this.topQueries = topQueries;
  }

  public List<StackGresDbOpsSamplingStatusQuery> getQueries() {
    return queries;
  }

  public void setQueries(List<StackGresDbOpsSamplingStatusQuery> queries) {
    this.queries = queries;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDbOpsSamplingStatus)) {
      return false;
    }
    StackGresDbOpsSamplingStatus other = (StackGresDbOpsSamplingStatus) obj;
    return Objects.equals(queries, other.queries) && Objects.equals(topQueries, other.topQueries);
  }

  @Override
  public int hashCode() {
    return Objects.hash(queries, topQueries);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.ValidEnum;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterUpdateStrategy {

  @ValidEnum(enumClass = StackGresClusterUpdateStrategyType.class, allowNulls = false,
      message = "type must be one of Always, Schedule, OnlyDbOps or Never")
  private String type;

  @ValidEnum(enumClass = StackGresClusterUpdateStrategyMethod.class, allowNulls = false,
      message = "method must be one of InPlace or ReducedImpact")
  private String method;

  @Valid
  private List<StackGresClusterUpdateStrategySchedule> schedule;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public List<StackGresClusterUpdateStrategySchedule> getSchedule() {
    return schedule;
  }

  public void setSchedule(List<StackGresClusterUpdateStrategySchedule> schedule) {
    this.schedule = schedule;
  }

  @Override
  public int hashCode() {
    return Objects.hash(method, schedule, type);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterUpdateStrategy)) {
      return false;
    }
    StackGresClusterUpdateStrategy other = (StackGresClusterUpdateStrategy) obj;
    return Objects.equals(method, other.method) && Objects.equals(schedule, other.schedule)
        && Objects.equals(type, other.type);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
